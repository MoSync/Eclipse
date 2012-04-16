package com.mobilesorcery.sdk.html5.debug;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Vector;

import org.eclipse.cdt.core.settings.model.util.Comparator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.Comment;
import org.eclipse.wst.jsdt.core.dom.DoStatement;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.ForInStatement;
import org.eclipse.wst.jsdt.core.dom.ForStatement;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.IfStatement;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.LabeledStatement;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.SwitchCase;
import org.eclipse.wst.jsdt.core.dom.SwitchStatement;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.WhileStatement;
import org.eclipse.wst.jsdt.core.dom.WithStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.DebuggerStatement;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.Pair;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.templates.Template;
import com.mobilesorcery.sdk.html5.Html5Plugin;

public class JSODDSupport {

	public class DebugRewriteOperationVisitor extends ASTVisitor {
		private final HashMap<Integer, List<ASTNode>> statementsToRewrite = new HashMap<Integer, List<ASTNode>>();
		private final HashMap<Integer, List<ASTNode>> functionPreambles = new HashMap<Integer, List<ASTNode>>();
		private final HashMap<Integer, List<ASTNode>> functionPostambles = new HashMap<Integer, List<ASTNode>>();
		private final HashMap<ASTNode, String> functionNames = new HashMap<ASTNode, String>();

		private JavaScriptUnit unit;
		private final Stack<ASTNode> statementStack = new Stack<ASTNode>();
		private LocalVariableScope currentScope = new LocalVariableScope()
				.nestScope();
		private final TreeMap<Integer, LocalVariableScope> localVariables = new TreeMap<Integer, LocalVariableScope>();
		private final HashSet<Integer> debuggerStatements = new HashSet<Integer>();
		private final HashSet<ASTNode> exclusions = new HashSet<ASTNode>();
		private final HashSet<Statement> blockifiables = new HashSet<Statement>();
		private final HashMap<Integer, NavigableMap<Integer, List<String>>> insertions = new HashMap<Integer, NavigableMap<Integer, List<String>>>();
		private final HashMap<ASTNode, JavaScriptUnit> nodeToUnitMap = new HashMap<ASTNode, JavaScriptUnit>();

		@Override
		public void preVisit(ASTNode node) {
			int start = getStartPosition(node);
			int startLine = unit == null ? -1 : unit.getLineNumber(start);

			LocalVariableScope startScope = currentScope;
			boolean nest = isNestStatement(node);

			blockify(node);

			if (node instanceof FunctionDeclaration) {
				FunctionDeclaration fd = (FunctionDeclaration) node;
				currentScope = currentScope.nestScope();
				for (Object paramObj : fd.parameters()) {
					SingleVariableDeclaration param = (SingleVariableDeclaration) paramObj;
					String name = param.getName().getIdentifier();
					currentScope = currentScope
							.addLocalVariableDeclaration(name);
				}
				SimpleName functionName = fd.getName();
				String functionIdentifier = functionName == null ? "<anonymous>" : functionName.getIdentifier();
				Block body = fd.getBody();
				List statements = body.statements();
				if (statements.isEmpty()) {
					// TODO!
				} else {
					ASTNode firstStatement = (ASTNode) statements.get(0);
					addStatementInstrumentationLocation(unit, firstStatement,
							functionPreambles, true, false);
					functionNames.put(firstStatement, functionIdentifier);
					ASTNode lastStatement = (ASTNode) statements
							.get(statements.size() - 1);
					addStatementInstrumentationLocation(unit, lastStatement,
							functionPostambles, false, false);
				}
				// Already nested!
				nest = false;
			}

			if (node instanceof SwitchStatement) {
				// The first statement in a switch statement
				// should always be a case-switch statement.
				SwitchStatement switchStatement = (SwitchStatement) node;
				List statements = switchStatement.statements();
				for (Object statementObj : statements) {
					if (statementObj instanceof SwitchCase) {
						addToExclusionList((SwitchCase) statementObj);
						continue;
					}
				}
			}

			if (node instanceof LabeledStatement) {
				// Labels aren't really executable, but their statements are.
				LabeledStatement labeledStatement = (LabeledStatement) node;
				addToExclusionList(labeledStatement.getBody());
			}

			if (node instanceof JavaScriptUnit) {
				unit = (JavaScriptUnit) node;
			}

			if (node instanceof Statement) {
				statementStack.push(node);
			}

			if (node instanceof ExpressionStatement) {
				Expression expression = ((ExpressionStatement) node).getExpression();
				if (expression instanceof SimpleName) {
					boolean isDebuggerStatement = "debugger".equals(((SimpleName) expression).getIdentifier());
					if (isDebuggerStatement) {
						debuggerStatements.add(startLine);
					}
				}
			}

			if (isInstrumentableStatement(node)) {
				addStatementInstrumentationLocation(unit, node,
						statementsToRewrite, true, true);
			}

			if (nest) {
				currentScope = currentScope.nestScope();
			}

			if (currentScope != startScope) {
				// It's ok to overwrite the previous localvariables if on the
				// same line.
				int end = getStartPosition(node) + getLength(node);
				int endLine = unit == null ? -1 : unit.getLineNumber(end);

				localVariables.put(endLine + 1, currentScope);
			}
		}

		private boolean isNestStatement(ASTNode node) {
			return node instanceof FunctionDeclaration ||
					node instanceof Block ||
					node instanceof ForStatement ||
					node instanceof ForInStatement;
		}

		private int getStartPosition(ASTNode node) {
			int pos = node.getStartPosition();
			// The JavaScriptDoc is always the first you get.
			int docLength = node.getLength() - getLength(node);
			return pos + docLength;
		}

		private int getLength(ASTNode node) {
			int docLength = 0;
			if (node instanceof BodyDeclaration) {
				BodyDeclaration bodyDeclaration = (BodyDeclaration) node;
				JSdoc doc = bodyDeclaration.getJavadoc();
				if (doc != null) {
					docLength = doc.getLength();
				}
			}

			return node.getLength() - docLength;
		}


		private boolean shouldBlockify(ASTNode node) {
			return blockifiables.contains(node);
		}

		private void blockify(ASTNode node) {
			if (node instanceof IfStatement) {
				IfStatement ifStatement = (IfStatement) node;
				addToBlockifyList(ifStatement.getThenStatement());
				addToBlockifyList(ifStatement.getElseStatement());
			} else if (node instanceof ForStatement) {
				ForStatement forStatement = (ForStatement) node;
				addToBlockifyList(forStatement.getBody());
			} else if (node instanceof ForInStatement) {
				ForInStatement forInStatement = (ForInStatement) node;
				addToBlockifyList(forInStatement.getBody());
			} else if (node instanceof WithStatement) {
				WithStatement withStatement = (WithStatement) node;
				addToBlockifyList(withStatement.getBody());
			} else if (node instanceof WhileStatement) {
				WhileStatement whileStatement = (WhileStatement) node;
				addToBlockifyList(whileStatement.getBody());
			} else if (node instanceof DoStatement) {
				DoStatement doStatement = (DoStatement) node;
				addToBlockifyList(doStatement.getBody());
			}
		}

		private void addToBlockifyList(Statement statement) {
			if (statement == null || statement instanceof Block) {
				// Already blockified!
				return;
			}

			blockifiables.add(statement);
		}

		private void addToExclusionList(ASTNode node) {
			if (node != null) {
				exclusions.add(node);
			}
		}

		private void addStatementInstrumentationLocation(JavaScriptUnit unit,
				ASTNode node, HashMap<Integer, List<ASTNode>> lineMap,
				boolean before, boolean maxOnePerLine) {
			if (unit == null) {
				return;
			}

			nodeToUnitMap.put(node, unit);

			Pair<Integer, Integer> pos = position(node, before);
			int insertionLine = pos.first;
			int insertionCol = pos.second;

			List<ASTNode> nodeList = lineMap.get(insertionLine);
			if (nodeList == null) {
				nodeList = new ArrayList<ASTNode>();
				lineMap.put(insertionLine, nodeList);
			}

			boolean shouldAdd = !maxOnePerLine;

			if (maxOnePerLine) {
				// If max one is allow, we try to insert the one with the lowest
				// column #.
				ASTNode previousNode = nodeList.isEmpty() ? null : nodeList.get(0);
				int previousCol = previousNode == null ? Integer.MAX_VALUE : position(previousNode, before).second;
				if (previousCol > insertionCol) {
					nodeList.clear();
					shouldAdd = true;
				}
			}

			if (insertionLine > 0 && insertionCol >= 0 && shouldAdd) {
				nodeList.add(node);
			}
		}

		private boolean isInstrumentableStatement(ASTNode node) {
			if (exclusions.contains(node)) {
				return false;
			}
			boolean isStatement = node instanceof Statement;
			boolean isBlock = node instanceof Block;
			boolean allowInstrumentation = isStatement && !isBlock;

			return allowInstrumentation;
		}

		@Override
		public void postVisit(ASTNode node) {
			LocalVariableScope startScope = currentScope;
			boolean unnest = isNestStatement(node);

			if (node instanceof VariableDeclarationFragment) {
				VariableDeclarationFragment localVar = (VariableDeclarationFragment) node;
				String name = localVar.getName().getIdentifier();
				currentScope = currentScope.addLocalVariableDeclaration(name);
			}

			if (node instanceof JavaScriptUnit) {
				unit = null;
			}

			if (node instanceof Statement) {
				statementStack.pop();
			}

			if (unnest) {
				currentScope = currentScope.unnestScope();
			}

			if (currentScope != startScope) {
				// It's ok to overwrite the previous localvariables if on the
				// same line.
				int end = getStartPosition(node) + getLength(node);
				int endLine = unit == null ? -1 : unit.getLineNumber(end);

				localVariables.put(endLine + 1, currentScope);
			}

			exclusions.remove(node);
		}

		private void insert(Pair<Integer, Integer> position, String text) {
			insert(position.first, position.second, text);
		}

		private void insert(int line, int col, String text) {
			if (line < 0 || col < 0) {
				return;
			}
			NavigableMap<Integer, List<String>> insertionsForLine = insertions
					.get(line);
			if (insertionsForLine == null) {
				insertionsForLine = new TreeMap<Integer, List<String>>();
				insertions.put(line, insertionsForLine);
			}
			List<String> insertionsForCol = insertionsForLine.get(col);
			if (insertionsForCol == null) {
				insertionsForCol = new ArrayList<String>();
				insertionsForLine.put(col, insertionsForCol);
			}
			insertionsForCol.add(text);
		}

		public void rewrite(long fileId, String source, Writer output,
				NavigableMap<Integer, LocalVariableScope> scopeMap)
				throws IOException {
			// TODO: I guess there is some better AST rewrite methods around.
			String[] lineByLineSource = source.split("\\n");
			StringBuffer result = new StringBuffer();

			int lineNo = 1;
			for (String line : lineByLineSource) {
				insertFunctionPreamble(fileId, lineNo, line);
				insertInstrumentedStatement(fileId, lineNo, line);
				insertFunctionPostamble(fileId, lineNo, line);
				lineNo++;
			}

			scopeMap.putAll(localVariables);

			lineNo = 1;
			for (String originalLine : lineByLineSource) {
				NavigableMap<Integer, List<String>> insertionList = insertions
						.get(lineNo);
				String newLine = originalLine;
				if (insertionList != null) {
					ArrayList<String> segments = new ArrayList<String>();
					int originalCol = 0;
					for (Integer col : insertionList.keySet()) {
						List<String> snippets = insertionList.get(col);
						if (originalCol < originalLine.length()) {
							segments.add(originalLine.substring(originalCol, col));
						}
						for (String snippet : snippets) {
							segments.add(snippet);
						}
						originalCol = col;
					}
					segments.add(originalLine.substring(originalCol));
					newLine = Util.join(segments.toArray(), "");
				}
				result.append(newLine);
				result.append('\n');
				lineNo++;
			}

			if (output != null) {
				output.write(result.toString());
			}
		}

		private void insertFunctionPreamble(long fileId, int lineNo, String line) {
			Collection<ASTNode> nodeList = functionPreambles.get(lineNo);
			if (nodeList == null) {
				return;
			}
			for (ASTNode node : nodeList) {
				String funcName = functionNames.get(node);
				insert(position(node, true), MessageFormat.format(
						"try '{' MoSyncDebugProtocol.pushStack(\"{0}\",{1},{2});",
						funcName, Long.toString(fileId),
						Integer.toString(lineNo)));
			}
		}

		private void insertFunctionPostamble(long fileId, int lineNo,
				String line) {
			Collection<ASTNode> nodeList = functionPostambles.get(lineNo);
			if (nodeList == null) {
				return;
			}
			for (ASTNode node : nodeList) {
				insert(position(node, false), "}" + "catch (anException) {"
						+ "if (!anException.alreadyThrown) {"
						+ "MoSyncDebugProtocol.reportException(anException);"
						+ "} anException.alreadyThrown = true;"
						+ "throw anException;} finally {"
						+ "MoSyncDebugProtocol.popStack();}");
			}
		}

		private Pair<Integer, Integer> position(ASTNode node, boolean start) {
			JavaScriptUnit unit = nodeToUnitMap.get(node);
			if (node == null) {
				return new Pair(-1, -1);
			}
			if (unit == null) {
				throw new IllegalStateException("Node has no matched unit");
			}
			int pos = start ? getStartPosition(node) : getStartPosition(node)
					+ getLength(node);
			int line = unit.getLineNumber(pos);
			int col = unit.getColumnNumber(pos);
			return new Pair(line, col);
		}

		private void insertInstrumentedStatement(long fileId, int lineNo,
				String line) {
			List<ASTNode> nodeList = statementsToRewrite.get(lineNo);
			// Max 1 statement instrumentation per line
			ASTNode node = (nodeList != null && !nodeList.isEmpty()) ? nodeList
					.get(0) : null;
			int col = position(node, true).second;

			if (node != null && col >= 0) {
				col = Math.min(col, line.length());

				if (shouldBlockify(node)) {
					insert(lineNo, col, "{");
					insert(position(node, false), "}");
				}

				Entry<Integer, LocalVariableScope> scope = localVariables
						.floorEntry(lineNo);
				String scopeDesc = "";
				if (scope != null) {
					scopeDesc = "/*" + scope.getValue().getLocalVariables()
							+ "*/";
				}

				boolean isDebuggerStatement = debuggerStatements.contains(lineNo);

				String addThis = scopeDesc
						+ " MoSyncDebugProtocol.updatePosition(" + fileId + ","
						+ lineNo + ","
						+ isDebuggerStatement + ","
						+ "function(____eval) {return eval(____eval);});"
						+ "\n";
				insert(lineNo, col, addThis);
			}
		}
	}

	public static final String SERVER_HOST_PROP = "SERVER_HOST";
	public static final String SERVER_PORT_PROP = "SERVER_PORT";

	private final ASTParser parser;

	private HashMap<IPath, Long> fileIds = null;
	private final TreeMap<Long, IPath> reverseFileIds = new TreeMap<Long, IPath>();
	private final HashMap<Long, NavigableMap<Integer, LocalVariableScope>> scopeMaps = new HashMap<Long, NavigableMap<Integer, LocalVariableScope>>();
	private long currentFileId = 0;

	private final IProject project;

	public JSODDSupport(IProject project) {
		this.project = project;
		applyDiff(null);
		parser = ASTParser.newParser(AST.JLS3);
	}

	public boolean applyDiff(IFileTreeDiff diff) {
		final boolean[] result = new boolean[1];
		if (diff == null) {
			try {
				project.accept(new IResourceVisitor() {
					@Override
					public boolean visit(IResource resource)
							throws CoreException {
						if (MoSyncBuilder.isInOutput(resource.getProject(),
								resource)) {
							return false;
						}
						IPath location = resource.getFullPath();
						result[0] |= (fileIds == null || fileIds.get(location) == null);
						assignFileId(location);
						return true;
					}
				});
			} catch (CoreException e) {
				// Bah. Just ignore.
				CoreMoSyncPlugin.getDefault().log(e);
			}
		} else {
			List<IPath> added = diff.getAdded();
			for (IPath path : added) {
				result[0] |= (fileIds == null || fileIds.get(path) == null);
				assignFileId(path);
			}
		}
		return result[0];
	}

	public void rewrite(IPath file, Writer output) throws CoreException {
		try {
			if (!isValidJavaScriptFile(file)) {
				return;
			}
			File absoluteFile = ResourcesPlugin.getWorkspace().getRoot()
					.findMember(file).getLocation().toFile();
			String source = Util.readFile(absoluteFile.getAbsolutePath());
			parser.setSource(source.toCharArray());
			ASTNode ast = parser.createAST(new NullProgressMonitor());
			DebugRewriteOperationVisitor visitor = new DebugRewriteOperationVisitor();
			ast.accept(visitor);
			long fileId = assignFileId(file);
			TreeMap<Integer, LocalVariableScope> scopeMap = new TreeMap<Integer, LocalVariableScope>();
			visitor.rewrite(fileId, source, output, scopeMap);
			scopeMaps.put(fileId, scopeMap);
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Html5Plugin.PLUGIN_ID, e.getMessage(), e));
		}
	}

	public static boolean isValidJavaScriptFile(IPath file) {
		// TODO: Use content descriptors!?
		// TODO: JS embedded in HTML
		return file != null && "js".equalsIgnoreCase(file.getFileExtension());
	}

	private long assignFileId(IPath file) {
		if (!isValidJavaScriptFile(file)) {
			return -1;
		}
		if (fileIds == null) {
			fileIds = new HashMap<IPath, Long>();
		}
		Long fileId = fileIds.get(file);
		if (fileId == null) {
			fileId = currentFileId;
			fileIds.put(file, fileId);
			reverseFileIds.put(fileId, file);
			currentFileId++;
		}

		return fileId;
	}

	private IPath getFile(long fileId) {
		return reverseFileIds.get(fileId);
	}

	public LocalVariableScope getScope(IFile file, int lineNo) {
		Long fileId = fileIds.get(file.getFullPath());
		if (fileId == null) {
			return null;
		}

		NavigableMap<Integer, LocalVariableScope> scopeMap = scopeMaps
				.get(fileId);
		if (scopeMap == null) {
			try {
				// TODO: Force build instead!!!
				rewrite(file.getFullPath(), null);
				scopeMap = scopeMaps.get(fileId);
			} catch (CoreException e) {
				// Gah.
			}
		}

		if (scopeMap != null) {
			Entry<Integer, LocalVariableScope> scope = scopeMap
					.floorEntry(lineNo - 1);
			if (scope != null) {
				return scope.getValue();
			}
		}

		return LocalVariableScope.EMPTY;
	}

	public void generateBoilerplate(IPropertyOwner projectProperties,
			Writer boilerplateOutput) throws CoreException {
		Template template = new Template(getClass().getResource(
				"/templates/jsoddsupport.template"));
		// TODO! Use Reload instead!
		Map<String, String> properties = getDefaultProperties();
		properties.putAll(projectProperties.getProperties());

		properties.put("INIT_FILE_IDS", generateFileIdInitCode());

		try {
			String contents = template.resolve(properties);
			boilerplateOutput.write(contents);
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Html5Plugin.PLUGIN_ID, e.getMessage(), e));
		}
	}

	public Map<String, String> getDefaultProperties() throws CoreException {
		HashMap<String, String> properties = new HashMap<String, String>();
		try {
			InetAddress localHost = InetAddress.getLocalHost();
			properties.put(SERVER_HOST_PROP, localHost.getHostAddress());
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Html5Plugin.PLUGIN_ID,
					"Could not determine localhost address"));
		}
		properties.put(SERVER_PORT_PROP, "8511");
		return properties;
	}

	private String generateFileIdInitCode() {
		StringBuffer result = new StringBuffer();
		for (Map.Entry<Long, IPath> entry : reverseFileIds.entrySet()) {
			result.append("idToFile[" + entry.getKey() + "]=\""
					+ entry.getValue().toPortableString() + "\";\n");
			result.append("fileToId[\"" + entry.getValue().toPortableString()
					+ "\"]=" + entry.getKey() + ";\n");
		}
		return result.toString();
	}
}
