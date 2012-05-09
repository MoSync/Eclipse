package com.mobilesorcery.sdk.html5.debug.rewrite;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;

import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.internal.compiler.ast.DebuggerStatement;

import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.html5.debug.JSODDSupport;
import com.mobilesorcery.sdk.html5.debug.LocalVariableScope;
import com.mobilesorcery.sdk.html5.debug.Position;

public class StatementRewrite extends NodeRewrite {

	private long fileId;
	private NavigableMap<Integer, LocalVariableScope> localVariables;
	private Set<ASTNode> blockifiables;
	private Set<Integer> instrumentedLines;

	public StatementRewrite(ISourceSupport rewriter, ASTNode node, long fileId, NavigableMap<Integer, LocalVariableScope> localVariables, Set<ASTNode> blockifiables, Set<Integer> instrumentedLines) {
		super(rewriter, node);
		this.fileId = fileId;
		this.localVariables = localVariables;
		this.blockifiables = blockifiables;
		this.instrumentedLines = instrumentedLines;
	}

	@Override
	public String rewrite(IFilter<String> features) {
		Position position = getPosition(node, true);
		int lineNo = position.getLine();
		Entry<Integer, LocalVariableScope> scope = localVariables.floorEntry(position.getPosition());
		String scopeDesc = "";
		if (scope != null) {
			scopeDesc = "/*" + scope.getValue().getLocalVariables()
					+ "*/";
		}
		
		boolean isInstrumentationSupported = supports(features, JSODDSupport.LINE_BREAKPOINTS);
		boolean isDebuggerStatement = false;
		if (isInstrumentationSupported && node instanceof ExpressionStatement) {
			Expression expression = ((ExpressionStatement) node)
					.getExpression();
			if (expression instanceof SimpleName) {
				isDebuggerStatement = "debugger"
						.equals(((SimpleName) expression).getIdentifier());
			}
		}
		
		String result = defaultRewrite(features);
		if (isInstrumentationSupported && !instrumentedLines.contains(lineNo)) {
			// Max one instrumentation per line!
			String addThisBefore = scopeDesc
					+ " MoSyncDebugProtocol.updatePosition(" + fileId
					+ "," + lineNo + "," + isDebuggerStatement
					+ "," + JSODDSupport.EVAL_FUNC_SNIPPET + ");" + "\n";
			result = addThisBefore + result;
			if (shouldBlockify()) {
				result = '{' + result + '}';
			}
		}
		instrumentedLines.add(lineNo);
		return result;
	}

	private boolean shouldBlockify() {
		return blockifiables.contains(node);
	}

}
