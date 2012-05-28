package com.mobilesorcery.sdk.html5.debug.rewrite;

import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;

import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.EmptyStatement;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.SimpleName;

import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.html5.debug.JSODDSupport;
import com.mobilesorcery.sdk.html5.debug.LocalVariableScope;
import com.mobilesorcery.sdk.html5.debug.Position;

public class StatementRewrite extends NodeRewrite {

	private static final String NESTED_FUNCTION_BUG = "JSDT internals makes it impossible to add breakpoints to or hot replace this piece of code (Reason: 'nested function bug')";
	
	private long fileId;
	private NavigableMap<Integer, LocalVariableScope> localVariables;
	private Set<ASTNode> blockifiables;
	private Map<Integer, NodeRewrite> instrumentedLines;
	private boolean forceInstrumentation;

	public StatementRewrite(ISourceSupport rewriter, ASTNode node, long fileId, NavigableMap<Integer, LocalVariableScope> localVariables, Set<ASTNode> blockifiables, Map<Integer, NodeRewrite> instrumentedLines, boolean forceInstrumentation) {
		super(rewriter, node);
		this.fileId = fileId;
		this.localVariables = localVariables;
		this.blockifiables = blockifiables;
		this.instrumentedLines = instrumentedLines;
		this.forceInstrumentation = forceInstrumentation;
	}

	@Override
	public void rewrite(IFilter<String> features, IRewrite rewrite) {
		ASTNode node = getNode();
		Position start = getPosition(node, true);
		rewrite.seek(start);
		
		if (hasNestedFunctionBug()) {
			setBlacklisted(NESTED_FUNCTION_BUG);
			rewrite.insert("/*" + NESTED_FUNCTION_BUG + "*/");
			return;
		}
		Position end = getPosition(node, false);
		int lineNo = start.getLine();
		Entry<Integer, LocalVariableScope> scope = localVariables.floorEntry(start.getPosition());
		String scopeDesc = "";
		if (scope != null) {
			// Skip this in output
			//scopeDesc = "/*" + scope.getValue().getLocalVariables()
			//		+ "*/";
		}
		
		boolean isInstrumentationSupported = supports(features, JSODDSupport.LINE_BREAKPOINTS);
		boolean isDebuggerStatement = false;
		if (node instanceof ExpressionStatement) {
			Expression expression = ((ExpressionStatement) node)
					.getExpression();
			if (expression instanceof SimpleName) {
				isDebuggerStatement = "debugger"
						.equals(((SimpleName) expression).getIdentifier());
			}
		}
		
		NodeRewrite instrumentor = instrumentedLines.get(lineNo);
		boolean canInstrumentThisLine = instrumentor == null || instrumentor == this;
		boolean doInstrument = forceInstrumentation || isDebuggerStatement || (isInstrumentationSupported && canInstrumentThisLine);

		if (doInstrument) {
			// Max one instrumentation per line! Except for debugger statements.
			String addThisBefore = scopeDesc
					+ " MoSyncDebugProtocol.updatePosition(" + fileId
					+ "," + lineNo + "," + isDebuggerStatement
					+ "," + JSODDSupport.EVAL_FUNC_SNIPPET + ");" + "\n";
			instrumentedLines.put(lineNo, this);
			
			if (shouldBlockify()) {
				rewrite.insert("{");
			}
			rewrite.insert(addThisBefore);
		}
		
		defaultRewrite(features, rewrite);
		
		rewrite.seek(end);
		if (doInstrument && shouldBlockify()) {
			rewrite.insert("}");
		}
	}

	private boolean hasNestedFunctionBug() {
		// The allegedly fixed JSDT bug #227489?
		if (getNode() instanceof EmptyStatement) {
			// Heuristic to find out whether this
			// is in fact, NOT an empty statement.
			if (getSource(getNode()).contains("(")) {
				return true;
			}
		}
		return false;
	}

	private boolean shouldBlockify() {
		return blockifiables.contains(getNode());
	}

}
