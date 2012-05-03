package com.mobilesorcery.sdk.html5.debug;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.InetAddress;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;

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
import org.eclipse.jface.text.Position;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
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
import org.eclipse.wst.jsdt.web.core.javascript.IJsTranslation;
import org.eclipse.wst.jsdt.web.core.javascript.JsTranslationAdapter;
import org.eclipse.wst.jsdt.web.core.javascript.JsTranslationAdapterFactory;
import org.eclipse.wst.jsdt.web.core.javascript.JsTranslator;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.IProvider;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Pair;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.templates.Template;
import com.mobilesorcery.sdk.html5.Html5Plugin;

public class JSODDSupport {

	private static final String DROP_TO_FRAME = "drop.to.frame";

	private class Position {
		private final ASTNode node;
		private final JavaScriptUnit unit;
		private final boolean before;

		public Position(ASTNode node, JavaScriptUnit unit, boolean before) {
			this.node = node;
			this.unit = unit;
			this.before = before;
		}

		public int getLine() {
			return unit.getLineNumber(getPosition());
		}

		public int getColumn() {
			return unit.getColumnNumber(getPosition());
		}

		public int getPosition() {
			return before ? node.getStartPosition() : (node.getStartPosition() + node.getLength());
		}
	}

	// TODO: Now we always keep the entire source tree in memory -- not necessarily a good thing...
	public class DebugRewriteOperationVisitor extends ASTVisitor implements IProvider<String, ASTNode> {
		
		private static final String EVAL_FUNC_SNIPPET = "function(____eval) {return eval(____eval);}";
		
		private final HashMap<Integer, List<Pair<ASTNode, Boolean>>> statementsToRewrite = new HashMap<Integer, List<Pair<ASTNode, Boolean>>>();
		private final HashMap<Integer, List<Pair<ASTNode, Boolean>>> functionPreambles = new HashMap<Integer, List<Pair<ASTNode, Boolean>>>();
		private final HashMap<Integer, List<Pair<ASTNode, Boolean>>> functionPostambles = new HashMap<Integer, List<Pair<ASTNode, Boolean>>>();
		private final HashMap<ASTNode, String> functionNames = new HashMap<ASTNode, String>();
		private final TreeMap<Integer, Integer> movedSourceMap = new TreeMap<Integer, Integer>();

		private JavaScriptUnit unit;
		private final Stack<ASTNode> statementStack = new Stack<ASTNode>();
		private LocalVariableScope currentScope = new LocalVariableScope()
				.nestScope();
		private final TreeMap<Integer, LocalVariableScope> localVariables = new TreeMap<Integer, LocalVariableScope>();
		private final HashSet<Integer> debuggerStatements = new HashSet<Integer>();
		private final HashSet<ASTNode> exclusions = new HashSet<ASTNode>();
		private final HashSet<ASTNode> blockifiables = new HashSet<ASTNode>();
		private final TreeMap<Integer, List<String>> insertions = new TreeMap<Integer, List<String>>();
		private final HashMap<ASTNode, JavaScriptUnit> nodeToUnitMap = new HashMap<ASTNode, JavaScriptUnit>();
		private TreeSet<Integer> scopeResetPoints;
		private String instrumented;

		@Override
		public void preVisit(ASTNode node) {
			int start = getStartPosition(node);
			int startLine = unit == null ? -1 : unit.getLineNumber(start);

			LocalVariableScope startScope = currentScope;

			Integer scopeResetPoint = scopeResetPoints.floor(start);
			if (scopeResetPoint != null) {
				scopeResetPoints.remove(scopeResetPoint);
				currentScope = currentScope.clear();
			}

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
				String functionIdentifier = functionName == null ? "<anonymous>"
						: functionName.getIdentifier();
				Block body = fd.getBody();
				List statements = body.statements();
				boolean emptyFunction = statements.isEmpty();
				if (statements.isEmpty()) {
					// Small hack here to work with nodes rather than file
					// positions...
					forceBlockify(body);
				}

				ASTNode firstStatement = emptyFunction ? body
						: (ASTNode) statements.get(0);
				addStatementInstrumentationLocation(unit, firstStatement,
						functionPreambles, true);
				functionNames.put(firstStatement, functionIdentifier);
				ASTNode lastStatement = emptyFunction ? body
						: (ASTNode) statements.get(statements.size() - 1);
				addStatementInstrumentationLocation(unit, lastStatement,
						functionPostambles, false);
				// If the } is on a separate line, let's add an extra
				// stop here -- will make stepping etc more useful.
				addStatementInstrumentationLocation(unit, lastStatement,
						statementsToRewrite, false);

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
				Expression expression = ((ExpressionStatement) node)
						.getExpression();
				if (expression instanceof SimpleName) {
					boolean isDebuggerStatement = "debugger"
							.equals(((SimpleName) expression).getIdentifier());
					if (isDebuggerStatement) {
						debuggerStatements.add(startLine);
					}
				}
			}

			if (isInstrumentableStatement(node)) {
				addStatementInstrumentationLocation(unit, node,
						statementsToRewrite, true);
			}

			if (nest) {
				currentScope = currentScope.nestScope();
			}

			if (currentScope != startScope) {
				localVariables.put(start, currentScope);
			}
		}

		private boolean isNestStatement(ASTNode node) {
			return node instanceof FunctionDeclaration || node instanceof Block
					|| node instanceof ForStatement
					|| node instanceof ForInStatement;
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

		private void forceBlockify(ASTNode node) {
			blockifiables.add(node);
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

		private void addToBlockifyList(ASTNode statement) {
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
				ASTNode node,
				HashMap<Integer, List<Pair<ASTNode, Boolean>>> lineMap,
				boolean before) {
			if (unit == null) {
				return;
			}

			nodeToUnitMap.put(node, unit);

			Position pos = position(node, before);
			int insertionLine = pos.getLine();
			int insertionCol = pos.getColumn();

			List<Pair<ASTNode, Boolean>> nodeList = lineMap.get(insertionLine);
			if (nodeList == null) {
				nodeList = new ArrayList<Pair<ASTNode, Boolean>>();
				lineMap.put(insertionLine, nodeList);
			}

			if (insertionLine > 0 && insertionCol >= 0) {
				nodeList.add(new Pair(node, before));
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
				int end = getStartPosition(node) + getLength(node);
				localVariables.put(end, currentScope);
			}

			exclusions.remove(node);
		}

		private void insert(Position position, String text) {
			int pos = position.getPosition();
			List<String> insertionsForPosition = insertions.get(pos);
			if (insertionsForPosition == null) {
				insertionsForPosition = new ArrayList<String>();
				insertions.put(pos, insertionsForPosition);
			}

			insertionsForPosition.add(text);
		}

		public void rewrite(long fileId, String originalSource, String prunedSource, Writer output,
				NavigableMap<Integer, LocalVariableScope> scopeMap, HashMap<String, IRedefinable> redefinables)
				throws IOException {
			// TODO: I guess there are some better AST rewrite methods around.
			LineMap lineByLinePrunedSource = new LineMap(prunedSource);
			LineMap lineByLineOriginalSource = new LineMap(originalSource);

			int lineNo = 1;
			int lineCount =  lineByLinePrunedSource.getLineCount();
			for (int i = 0; i < lineCount; i++) {
				insertFunctionPreamble(fileId, lineNo, lineByLineOriginalSource);
				insertInstrumentedStatement(fileId, lineNo, lineByLineOriginalSource);
				insertFunctionPostamble(fileId, lineNo);
				lineNo++;
			}

			for (Map.Entry<Integer, LocalVariableScope> scope : localVariables.entrySet()) {
				int mappedLineNo = lineByLineOriginalSource.getLine(scope.getKey());
				scopeMap.put(mappedLineNo, scope.getValue());
			}

			StringBuffer result = new StringBuffer();

			int prevPos = 0;
			int posDelta = 0;
			for (Map.Entry<Integer, List<String>> insertion : insertions.entrySet()) {
				Integer pos = insertion.getKey();
				List<String> insertionsAtPos = insertion.getValue();
				result.append(originalSource.substring(prevPos, pos));
				String insertionStr = Util.join(insertionsAtPos.toArray(), ""); 
				result.append(insertionStr);
				prevPos = pos;
				posDelta += insertionStr.length();
				movedSourceMap.put(prevPos, posDelta);
			}
			result.append(originalSource.substring(prevPos));
			instrumented = result.toString();
			if (output != null) {
				output.write(instrumented);
			}
		}

		private void insertFunctionPreamble(long fileId, int lineNo, LineMap lineMap) {
			Collection<Pair<ASTNode, Boolean>> nodeList = functionPreambles
					.get(lineNo);
			if (nodeList == null) {
				return;
			}
			for (Pair<ASTNode, Boolean> nodePosition : nodeList) {
				ASTNode node = nodePosition.first;
				String funcName = functionNames.get(node);
				Position position = position(node, true);
				if (shouldBlockify(node)) {
					insert(position, "{");
				}
				int mappedLineNo = lineMap.getLine(position.getPosition());
				if (supports(DROP_TO_FRAME)) {
					insert(position, "do {");
				}
				insert(position,
						MessageFormat
								.format("try '{' MoSyncDebugProtocol.pushStack(\"{0}\",{1},{2});",
										funcName, Long.toString(fileId),
										Integer.toString(mappedLineNo)));
			}
		}

		private void insertFunctionPostamble(long fileId, int lineNo) {
			Collection<Pair<ASTNode, Boolean>> nodeList = functionPostambles
					.get(lineNo);
			if (nodeList == null) {
				return;
			}
			for (Pair<ASTNode, Boolean> nodePosition : nodeList) {
				ASTNode node = nodePosition.first;
				Position position = position(node, false);
				insert(position, "}" + "catch (anException) {"
						+ "if (!anException.alreadyThrown && !anException.dropToFrame) {"
						+ "anException = MoSyncDebugProtocol.reportException(anException, " + lineNo + "," + EVAL_FUNC_SNIPPET + ");"
						+ "} anException.alreadyThrown = true;"
						+ "if (!anException.dropToFrame) {"
						+ "throw anException;}else{"
						+ "if (anException.expression) {"
						+ "eval(anException.expression);}"
						+ "}} finally {"
						+ "MoSyncDebugProtocol.popStack();"
						+ "}");
				if (supports(DROP_TO_FRAME)) {
					insert(position, "} while (MoSyncDebugProtocol.dropToFrame());");
				}
				if (shouldBlockify(node)) {
					insert(position, "}");
				}
			}
		}

		private Position position(ASTNode node, boolean start) {
			JavaScriptUnit unit = nodeToUnitMap.get(node);
			if (unit == null) {
				throw new IllegalStateException("Node has no matched unit");
			}
			return new Position(node, unit, start);
		}

		private void insertInstrumentedStatement(long fileId, int lineNo, LineMap lineMap) {
			List<Pair<ASTNode, Boolean>> nodeList = statementsToRewrite
					.get(lineNo);

			if (nodeList != null) {
				TreeMap<Integer, Pair<ASTNode, Boolean>> nodesToInstrument = new TreeMap<Integer, Pair<ASTNode, Boolean>>();
				// Max 1 instrumentation per line.
				for (Pair<ASTNode, Boolean> node : nodeList) {
					int mappedLineNo = lineMap.getLine(position(node.first, node.second).getPosition());
					Pair<ASTNode, Boolean> nodeToInstrument = nodesToInstrument.get(mappedLineNo);
					if (nodeToInstrument == null || position(nodeToInstrument.first, nodeToInstrument.second).getPosition() > position(node.first, node.second).getPosition()) {
						nodesToInstrument.put(mappedLineNo, node);
					}
				}

				for (Map.Entry<Integer, Pair<ASTNode, Boolean>> nodePosition : nodesToInstrument.entrySet()) {
					ASTNode node = nodePosition.getValue().first;
					boolean before = nodePosition.getValue().second;

					if (shouldBlockify(node)) {
						insert(position(node, true), "{");
						insert(position(node, false), "}");
					}
					int mappedLineNo = nodePosition.getKey();

					Entry<Integer, LocalVariableScope> scope = localVariables
							.floorEntry(position(node, true).getPosition());
					String scopeDesc = "";
					if (scope != null) {
						scopeDesc = "/*" + scope.getValue().getLocalVariables()
								+ "*/";
					}

					boolean isDebuggerStatement = debuggerStatements
							.contains(lineNo);

					String addThis = scopeDesc
							+ " MoSyncDebugProtocol.updatePosition(" + fileId + ","
							+ mappedLineNo + "," + isDebuggerStatement + ","
							+ EVAL_FUNC_SNIPPET + ");"
							+ "\n";
					insert(position(node, before), addThis);
				}
			}
		}

		public void setScopeResetPoints(TreeSet<Integer> scopeResetPoints) {
			this.scopeResetPoints = new TreeSet<Integer>(scopeResetPoints);
		}
		
		public String get(ASTNode node) {
			Position startPos = position(node, true);
			Position endPos = position(node, false);
			int start = startPos.getPosition();
			int end = endPos.getPosition();
			Entry<Integer, Integer> startDeltaEntry = movedSourceMap.floorEntry(start - 1);
			Entry<Integer, Integer> endDeltaEntry = movedSourceMap.floorEntry(end);
			int startDelta = startDeltaEntry == null ? 0 : startDeltaEntry.getValue();
			int endDelta = endDeltaEntry == null ? 0 : endDeltaEntry.getValue();
			return instrumented.substring(start + startDelta, end + endDelta - (start + startDelta));
		}

		public String getInstrumentedSource() {
			return instrumented;
		}
	}

	public static final String SERVER_HOST_PROP = "SERVER_HOST";
	public static final String SERVER_PORT_PROP = "SERVER_PORT";
	public static final String PROJECT_NAME_PROP = "PROJECT_NAME";

	private static final Map<String, IRedefinable> EMPTY = Collections.emptyMap();

	private final ASTParser parser;

	private HashMap<IPath, Long> fileIds = null;
	private final TreeMap<Long, IPath> reverseFileIds = new TreeMap<Long, IPath>();
	private final HashMap<Long, NavigableMap<Integer, LocalVariableScope>> scopeMaps = new HashMap<Long, NavigableMap<Integer, LocalVariableScope>>();
	private final HashMap<IFile, String> instrumentedSource = new HashMap<IFile, String>();
	
	private long currentFileId = 0;

	private final IProject project;

	public JSODDSupport(IProject project) {
		this.project = project;
		applyDiff(null);
		parser = ASTParser.newParser(AST.JLS3);
	}

	public boolean supports(String feature) {
		return true;
	}

	public boolean applyDiff(IFileTreeDiff diff) {
		final boolean[] result = new boolean[1];
		if (diff == null) {
			try {
				if (fileIds == null) {
					fileIds = new HashMap<IPath, Long>();
				}
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

	public Map<String, IRedefinable> rewrite(IPath filePath, Writer output) throws CoreException {
		try {
			if (!isValidJavaScriptFile(filePath)) {
				return EMPTY;
			}

			IFile file = (IFile) ResourcesPlugin.getWorkspace().getRoot()
					.findMember(filePath);
			File absoluteFile = file.getLocation().toFile();

			String source = Util.readFile(absoluteFile.getAbsolutePath());
			String prunedSource = source;
			TreeSet<Integer> scopeResetPoints = new TreeSet<Integer>();

			if (isEmbeddedJavaScriptFile(filePath)) {
				prunedSource = getEmbeddedJavaScript(file, scopeResetPoints);
			}

			parser.setSource(prunedSource.toCharArray());
			ASTNode ast = parser.createAST(new NullProgressMonitor());
			DebugRewriteOperationVisitor visitor = new DebugRewriteOperationVisitor();
			visitor.setScopeResetPoints(scopeResetPoints);
			ast.accept(visitor);
			long fileId = assignFileId(filePath);
			TreeMap<Integer, LocalVariableScope> scopeMap = new TreeMap<Integer, LocalVariableScope>();
			HashMap<String, IRedefinable> result = new HashMap<String, IRedefinable>();
			visitor.rewrite(fileId, source, prunedSource, output, scopeMap, result);
			String instrumentedSource = visitor.getInstrumentedSource();
			this.instrumentedSource.put(file, instrumentedSource);
			scopeMaps.put(fileId, scopeMap);
			return result;
		} catch (CoreException e) {
			throw e;
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Html5Plugin.PLUGIN_ID, e.getMessage(), e));
		}
	}

	// Returns a string where everything that is not javascript is replace by spaces
	private String getEmbeddedJavaScript(IFile file, NavigableSet<Integer> scopeResetPoints) throws IOException, CoreException, InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		IModelManager modelManager = StructuredModelManager
				.getModelManager();
		IStructuredDocument doc = modelManager
				.createStructuredDocumentFor(file);
		JsTranslator translator = new JsTranslator(doc, file
				.getFullPath().toOSString()) {
			@Override
			public void finishedTranslation() {
				super.finishedTranslation();
				latch.countDown();
			}
		};
		translator.translate();
		latch.await();
		// This is because we parse embedded js as one file; we need to know
		// where to reset the scopes. Overly complicated...
		org.eclipse.jface.text.Position[] htmlLocations = translator.getHtmlLocations();
		for (org.eclipse.jface.text.Position htmlLocation : htmlLocations) {
			scopeResetPoints.add(htmlLocation.offset);
		}
		return translator.getJsText();
	}

	public static boolean isValidJavaScriptFile(IPath file) {
		// TODO: Use content descriptors!?
		// TODO: JS embedded in HTML
		return isEmbeddedJavaScriptFile(file)
				|| (file != null && "js".equalsIgnoreCase(file
						.getFileExtension()));
	}

	public static boolean isEmbeddedJavaScriptFile(IPath file) {
		return file != null && "html".equalsIgnoreCase(file.getFileExtension());
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

	public void generateBoilerplate(MoSyncProject project,
			Writer boilerplateOutput) throws CoreException {
		Template template = new Template(getClass().getResource(
				"/templates/jsoddsupport.template"));
		// TODO! Use Reload instead!
		Map<String, String> properties = getDefaultProperties();
		properties.putAll(project.getProperties());

		properties.put("INIT_FILE_IDS", generateFileIdInitCode());
		properties.put("PROJECT_NAME", project.getName());
		properties.put("TIMEOUT_IN_MS", "5000");

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

	public Set<IPath> getAllFiles() {
		if (fileIds == null) {
			applyDiff(null);
		}
		Set<IPath> allPaths = new HashSet<IPath>(fileIds.keySet());
		return allPaths;
	}

	public String getInstrumentedSource(IFile file) {
		return instrumentedSource.get(file);
	}
}
