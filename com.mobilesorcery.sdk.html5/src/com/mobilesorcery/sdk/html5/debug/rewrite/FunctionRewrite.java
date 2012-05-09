package com.mobilesorcery.sdk.html5.debug.rewrite;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Statement;

import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.html5.debug.JSODDSupport;
import com.mobilesorcery.sdk.html5.debug.Position;

public class FunctionRewrite extends NodeRewrite {

	private long fileId;
	private Map<ASTNode, String> nodeRedefinables;

	public FunctionRewrite(ISourceSupport rewriter, ASTNode node, long fileId, Map<ASTNode, String> nodeRedefinables) {
		super(rewriter, node);
		this.fileId = fileId;
		this.nodeRedefinables = nodeRedefinables;
	}

	@Override
	public String rewrite(IFilter<String> features) {
		FunctionDeclaration fd = (FunctionDeclaration) node;
		Position startOfBody = getPosition(fd.getBody(), true);
		Position endOfBody = getPosition(fd.getBody(), true);
		int startLineOfBody = startOfBody.getLine();
		int endLineOfBody = endOfBody.getLine();
		SimpleName functionName = fd.getName();
		boolean isAnonymous = isAnonymous(fd);
		String functionIdentifier = isAnonymous ? "<anonymous>"
				: functionName.getIdentifier();
		
		String functionStart = MessageFormat.format(
				"try '{' MoSyncDebugProtocol.pushStack(\"{0}\",{1},{2});",
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
				+ "}\n";

		int fdStart = fd.getStartPosition();
		int bodyStart = fd.getBody().getStartPosition();

		String signature = rewriter.getSource(fdStart, bodyStart);

		String dropToFramePreamble = supports(features, JSODDSupport.DROP_TO_FRAME) ? "do {"
				: "";
		String dropToFramePostamble = supports(features, JSODDSupport.DROP_TO_FRAME) ? "} while (MoSyncDebugProtocol.dropToFrame());"
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
					+ functionRef + "=" + signature + "{";

			List parameters = fd.parameters();
			String[] parameterNames = new String[parameters.size()];
			for (int i = 0; i < parameterNames.length; i++) {
				SingleVariableDeclaration parameter = (SingleVariableDeclaration) parameters
						.get(i);
				String parameterName = parameter.getName().getIdentifier();
				parameterNames[i] = parameterName;
			}

			editAndContinuePostamble = "}\nMoSyncDebugProtocol.registerFunction(\"" + redefineKey
					+ "\"," + functionIdentifier + ");}\n" + functionRef + "("
					+ Util.join(parameterNames, ",") + ");}";
		}

		StringBuffer result = new StringBuffer();
		result.append(signature);
		result.append("\n{");
		result.append(dropToFramePreamble);
		result.append(functionStart);
		result.append(editAndContinuePreamble);
		Block block = fd.getBody();
		List statements = block.statements();
		for (Object statementObj : statements) {
			ASTNode statement = (ASTNode) statementObj;
			result.append(getRewrite(statement));
		}
		result.append(editAndContinuePostamble);
		result.append(functionEnd);
		result.append(dropToFramePostamble);
		result.append("\n}");

		return result.toString();
	}

	private boolean isAnonymous(FunctionDeclaration fd) {
		return fd.getName() == null;
	}
}
