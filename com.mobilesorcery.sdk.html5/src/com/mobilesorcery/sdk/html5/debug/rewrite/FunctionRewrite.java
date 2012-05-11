package com.mobilesorcery.sdk.html5.debug.rewrite;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.CatchClause;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.ReturnStatement;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.TryStatement;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.wst.jsdt.internal.compiler.util.SimpleNameVector;

import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.html5.debug.JSODDSupport;
import com.mobilesorcery.sdk.html5.debug.Position;

public class FunctionRewrite extends NodeRewrite {

	private final class HasReturnASTVisitor extends ASTVisitor {
		private int inInInnerFunction = 0;
		private boolean hasReturn = false;

		public void preVisit(ASTNode node) {
			if (node instanceof FunctionDeclaration) {
				inInInnerFunction++;
			}
			if (node instanceof ReturnStatement) {
				if (inInInnerFunction == 0 && ((ReturnStatement) node).getExpression() != null) {
					hasReturn = true;
				}
			}
		}

		public void postVisit(ASTNode node) {
			if (node instanceof FunctionDeclaration) {
				inInInnerFunction--;
			}
		}
	}

	private long fileId;
	private Map<ASTNode, String> nodeRedefinables;
	private Block body;

	public FunctionRewrite(ISourceSupport rewriter, ASTNode node, long fileId, Map<ASTNode, String> nodeRedefinables) {
		super(rewriter, node);
		if (!(node instanceof FunctionDeclaration)) {
			throw new IllegalArgumentException("Must use the body of the function");
		}
		this.fileId = fileId;
		this.nodeRedefinables = nodeRedefinables;
	}

	@Override
	public void rewrite(IFilter<String> features, IRewrite rewrite) {
		FunctionDeclaration fd = (FunctionDeclaration) getNode();
		Block body = fd.getBody();
		// Strange parsing problems in some instances!
		if (isOutsideParent(body)) {
			// TODO: REPORT ERROR!!!
			return;
		}
		List statements = body.statements();
		if (hasEndOfBodyBug(body, statements)) {
			return;
		}
		Position startOfBody = getPosition(body, true);
		Position endOfBody = getPosition(body, false);
		boolean emptyBody = statements.isEmpty();
		
		int startLineOfBody = startOfBody.getLine();
		int endLineOfBody = endOfBody.getLine();

		SimpleName functionName = fd.getName();
		boolean isAnonymous = isAnonymous(fd);
		String functionIdentifier = isAnonymous ? "<anonymous>"
				: functionName.getIdentifier();
		/*
		AST ast = rewrite.getAST();
		ASTNode fdCopy = rewrite.createCopyTarget(fd);
		Block newBody = ast.newBlock();
		Block tryBody = ast.newBlock();
		
		String pushStackStatementStr = MessageFormat.format("MoSyncDebugProtocol.pushStack(\"{0}\",{1},{2});",
				functionIdentifier, Long.toString(fileId),
				Integer.toString(startLineOfBody));
		ASTNode pushStackStatement = rewrite.createStringPlaceholder(pushStackStatementStr, ASTNode.EXPRESSION_STATEMENT);
		tryBody.statements().add(pushStackStatement);
		TryStatement functionSurround = ast.newTryStatement();
		functionSurround.setBody(tryBody);
		CatchClause catchClause = ast.newCatchClause();
		SingleVariableDeclaration exceptionDecl = ast.newSingleVariableDeclaration();
		exceptionDecl.setName(ast.newSimpleName("anException"));
		Block catchBody = ast.newBlock();
		
		String exceptionHandlerStr = "if (!anException.alreadyThrown && !anException.dropToFrame) {\n"
				+ "    anException = MoSyncDebugProtocol.reportException(anException, " + endLineOfBody + "," + JSODDSupport.EVAL_FUNC_SNIPPET + ");\n"
				+ "  }\n"
				+ "  anException.alreadyThrown = true;\n"
				+ "  if (!anException.dropToFrame) {\n"
				+ "    throw anException;\n"
				+ "  } else {\n"
				+ "    if (anException.expression) {\n"
				+ "      eval(anException.expression);"
				+ "    }\n"
				+ "  }\n";
		rewrite.createStringPlaceholder(exceptionHandlerStr, ASTNode.)
		BAHH!!!
		
		catchClause.setBody(catchBody);
		functionSurround.catchClauses().add(catchClause);
		newBody.statements().add(functionSurround);
		
		rewrite.replace(body, newBody, null);*/
		boolean shouldBlockify = !supports(features, JSODDSupport.DROP_TO_FRAME) && !supports(features, JSODDSupport.EDIT_AND_CONTINUE);
		
		String functionStart = MessageFormat.format(
				"'{' try '{' MoSyncDebugProtocol.pushStack(\"{0}\",{1},{2});",
				functionIdentifier, Long.toString(fileId),
				Integer.toString(startLineOfBody));
		String functionEnd = "} catch (anException) {\n"
				+ "  if (!anException.alreadyThrown && !anException.dropToFrame) {\n"
				+ "    anException = MoSyncDebugProtocol.reportException(anException, " + endLineOfBody + "," + JSODDSupport.EVAL_FUNC_SNIPPET + ");\n"
				+ "  }\n"
				+ "  anException.alreadyThrown = true;\n"
				+ "  if (!anException.dropToFrame) {\n"
				+ "    throw anException;\n"
				+ "  } else {\n"
				+ "    if (anException.expression) {\n"
				+ "      eval(anException.expression);"
				+ "    }\n"
				+ "  }\n"
				+ "} finally {\n"
				+ "  MoSyncDebugProtocol.popStack();\n"
				+ "}\n}\n";

		String dropToFramePreamble = supports(features, JSODDSupport.DROP_TO_FRAME) ? "{ do"
				: "";
		String dropToFramePostamble = supports(features, JSODDSupport.DROP_TO_FRAME) ? "while (MoSyncDebugProtocol.dropToFrame()); }"
				: "";

		String editAndContinuePreamble = "";
		String editAndContinuePostamble = "";
		if (supports(features, JSODDSupport.EDIT_AND_CONTINUE) && !isAnonymous(fd)) {
			String functionRef = functionIdentifier + ".____yaloid";
			String redefineKey = nodeRedefinables.get(fd);
			editAndContinuePreamble = "if(!" + functionRef + ") { "
					+ "var ____unevaled=MoSyncDebugProtocol.yaloid(\""
					+ redefineKey + "\");\n" + "if (____unevaled){\n"
					+ "eval(\"" + functionRef + "=\" + ____unevaled);}\n"
					+ "if(typeof " + functionRef + " !== \'function\')\n" + "{"
					+ functionRef + "= function " + getSignature(fd);

			List parameters = fd.parameters();
			String[] parameterNames = new String[parameters.size()];
			for (int i = 0; i < parameterNames.length; i++) {
				SingleVariableDeclaration parameter = (SingleVariableDeclaration) parameters
						.get(i);
				String parameterName = parameter.getName().getIdentifier();
				parameterNames[i] = parameterName;
			}

			String returnStr = hasReturn(fd.getBody()) ? "return " : "";
			editAndContinuePostamble = "\nMoSyncDebugProtocol.registerFunction(\"" + redefineKey
					+ "\"," + functionIdentifier + ");}}\n" + returnStr + functionRef + "("
					+ Util.join(parameterNames, ",") + ");";
		}

		rewrite.seek(startOfBody);
		if (shouldBlockify) {
			//rewrite.insert("\n{");
		}
		
		rewrite.insert(dropToFramePreamble);
		if (supports(features, JSODDSupport.ARTIFICIAL_STACK)) {
			rewrite.insert(functionStart);
		}
		rewrite.insert(editAndContinuePreamble);
		for (Object statementObj : statements) {
			ASTNode statement = (ASTNode) statementObj;
			getRewrite(statement).rewrite(features, rewrite);
		}
		rewrite.seek(endOfBody);
		rewrite.insert(editAndContinuePostamble);
		if (supports(features, JSODDSupport.ARTIFICIAL_STACK)) {
			rewrite.insert(functionEnd);
		}
		rewrite.insert(dropToFramePostamble);
		if (shouldBlockify) {
			//rewrite.insert("\n}");
		}
	}

	private boolean hasEndOfBodyBug(Block body, List statements) {
		// Another strange JSDT bug -- and now
		// don't dare using ASTRewrite; what
		// if the same bug is there!?
		// Sometimes the node length of a function is too small!
		// This is a non-bullet proof way to do it.
		Position result = getPosition(body, false);
		for (Object statementObj : statements) {
			ASTNode statement = (ASTNode) statementObj;
			Position endOfStatement = getPosition(statement, false);
			if (endOfStatement.getPosition() > result.getPosition()) {
				return true;
			}
		}
		return false;
	}

	private boolean isOutsideParent(Block body) {
		// Note: A body declaration CAN be outside its parent,
		// but blocks cannot.
		int startOfBody = body.getStartPosition();
		ASTNode parent = body.getParent();
		while (parent != null) {
			if (parent.getStartPosition() > startOfBody) {
				return true;
			}
			parent = parent.getParent();
		}
		return false;
	}

	private String getSignature(FunctionDeclaration fd) {
		// Bah. Again: bah.
		ASTNode node = findFirstNode(fd.modifiers(), fd.getReturnType2(), fd.getName(), fd.parameters());
		if (node == null) {
			// Anonymous, zero-arg function
			return "()";
		} else {
			int start = node.getStartPosition();
			int end = fd.getBody().getStartPosition();
			return rewriter.getSource(start, end);
		}
	}

	private ASTNode findFirstNode(Object... nodes) {
		for (int i = 0; i < nodes.length; i++) {
			Object nodeObj = nodes[i];
			if (nodeObj instanceof List) {
				ASTNode result = findFirstNode(((List) nodeObj).toArray());
				if (result != null) {
					return result;
				}
			} else {
				ASTNode node = (ASTNode) nodeObj;
				if (node != null) {
					return node;
				}
			}
		}
		return null;
	}

	public ASTNode getReplacedNode() {
		return body;
	}
	
	private boolean isAnonymous(FunctionDeclaration fd) {
		return fd.getName() == null;
	}
	
	private boolean hasReturn(Block statements) {
		HasReturnASTVisitor visitor = new HasReturnASTVisitor();
		statements.accept(visitor);
		return visitor.hasReturn;
	}
}
