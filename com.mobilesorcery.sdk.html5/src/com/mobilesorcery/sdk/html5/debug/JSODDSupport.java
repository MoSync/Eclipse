package com.mobilesorcery.sdk.html5.debug;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
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
import java.util.concurrent.CopyOnWriteArrayList;
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
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.CatchClause;
import org.eclipse.wst.jsdt.core.dom.DoStatement;
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
import org.eclipse.wst.jsdt.core.dom.ThisExpression;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.WhileStatement;
import org.eclipse.wst.jsdt.core.dom.WithStatement;
import org.eclipse.wst.jsdt.debug.core.breakpoints.IJavaScriptLineBreakpoint;
import org.eclipse.wst.jsdt.debug.core.model.JavaScriptDebugModel;
import org.eclipse.wst.jsdt.web.core.javascript.JsTranslator;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.xml.core.internal.parser.regions.TagNameRegion;
import org.eclipse.wst.xml.core.internal.text.XMLStructuredDocumentRegion;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Pair;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.templates.Template;
import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.hotreplace.FileRedefinable;
import com.mobilesorcery.sdk.html5.debug.hotreplace.FunctionRedefinable;
import com.mobilesorcery.sdk.html5.debug.hotreplace.HTMLRedefinable;
import com.mobilesorcery.sdk.html5.debug.hotreplace.ProjectRedefinable;
import com.mobilesorcery.sdk.html5.debug.rewrite.CatchRewrite;
import com.mobilesorcery.sdk.html5.debug.rewrite.FunctionRewrite;
import com.mobilesorcery.sdk.html5.debug.rewrite.ISourceSupport;
import com.mobilesorcery.sdk.html5.debug.rewrite.NodeRewrite;
import com.mobilesorcery.sdk.html5.debug.rewrite.SourceRewrite;
import com.mobilesorcery.sdk.html5.debug.rewrite.StatementRewrite;
import com.mobilesorcery.sdk.html5.debug.rewrite.ThisRewrite;

public class JSODDSupport {

	public class InvalidASTException extends RuntimeException {

		public InvalidASTException(String msg) {
			super(msg);
		}

	}

	public static final String DROP_TO_FRAME = "drop.to.frame";
	public static final String EDIT_AND_CONTINUE = "edit.continue";
	public static final String LINE_BREAKPOINTS = "line.breakpoint";
	public static final String ARTIFICIAL_STACK = "artificial.stack";

	public static final String EVAL_FUNC_SNIPPET = "function(____eval) {return eval(____eval);}";

	// TODO: Now we always keep the entire source tree in memory -- not
	// necessarily a good thing...
	public class DebugRewriteOperationVisitor extends ASTVisitor implements
			ISourceSupport {

		private static final int INSTRUMENTATION_DISALLOWED = 0;
		private static final int INSTRUMENTATION_ALLOWED = 1;
		private static final int FORCE_INSTRUMENTATION = 2;
		private static final int INSTRUMENTATION_BLACKLISTED = 3;
		private static final int ERROR_COUNT_THRESHOLD = 10;

		private JavaScriptUnit unit;
		private LocalVariableScope currentScope = new LocalVariableScope()
				.nestScope();
		private final Stack<FunctionRewrite> functionRewriteStack = new Stack<FunctionRewrite>();
		private final Stack<IRedefinable> redefinableStack = new Stack<IRedefinable>();
		private final TreeMap<Integer, LocalVariableScope> localVariables = new TreeMap<Integer, LocalVariableScope>();
		private final HashSet<ASTNode> exclusions = new HashSet<ASTNode>();
		private final HashSet<ASTNode> blockifiables = new HashSet<ASTNode>();
		private final TreeMap<Integer, List<String>> insertions = new TreeMap<Integer, List<String>>();
		private final HashMap<ASTNode, JavaScriptUnit> nodeToUnitMap = new HashMap<ASTNode, JavaScriptUnit>();
		private final HashMap<ASTNode, String> nodeRedefinables = new HashMap<ASTNode, String>();
		private final HashMap<String, IRedefinable> redefinables = new HashMap<String, IRedefinable>();

		private TreeSet<Integer> scopeResetPoints;
		private String instrumented;

		private HashMap<ASTNode, NodeRewrite> rewrites = new HashMap<ASTNode, NodeRewrite>();

		private String originalSource;
		private long fileId;
		private HashMap<Integer, NodeRewrite> instrumentedLines = new HashMap<Integer, NodeRewrite>();
		private LineMap lineMap;
		private Position currentPosition;

		public DebugRewriteOperationVisitor(LineMap lineMap, long fileId) {
			this.lineMap = lineMap;
			this.fileId = fileId;
		}

		@Override
		public void preVisit(ASTNode node) {
			currentPosition = getPosition(node, true);
			
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
				// Special function var.
				currentScope = currentScope.addLocalVariableDeclaration("arguments");
				for (Object paramObj : fd.parameters()) {
					SingleVariableDeclaration param = (SingleVariableDeclaration) paramObj;
					String name = param.getName().getIdentifier();
					currentScope = currentScope
							.addLocalVariableDeclaration(name);
				}

				SimpleName functionName = fd.getName();
				boolean isAnonymous = isAnonymous(fd);
				String functionIdentifier = isAnonymous ? Html5Plugin.ANONYMOUS_FUNCTION
						: functionName.getIdentifier();

				FunctionRewrite functionRewrite = new FunctionRewrite(this, fd, fileId,nodeRedefinables);
				rewrites.put(fd, functionRewrite);
				functionRewriteStack.push(functionRewrite);

				Block body = fd.getBody();

				ASTNode firstStatement = startOfFunction(fd);
				ASTNode lastStatement = endOfFunction(fd);
				
				// TODO: If both first and last statement null, then we have a problem
				// and should blacklist that node.

				if (firstStatement instanceof Block
						&& lastStatement instanceof Block) {
					forceBlockify(body);
				}
				// Add this to the set of redefinables
				pushRedefinable(new FunctionRedefinable(currentRedefinable(),
						this, node), fd);

				// Already nested!
				nest = false;
			}

			if (node instanceof CatchClause) {
				rewrites.put(node, new CatchRewrite(this, node));
			}

			checkForExclusion(node);
			checkBlacklist(node);

			if (node instanceof JavaScriptUnit) {
				unit = (JavaScriptUnit) node;
				validateAST(unit.getProblems());
			}

			int instrumentable = isInstrumentableStatement(node);
			if (instrumentable != INSTRUMENTATION_DISALLOWED) {
				rewrites.put(node, new StatementRewrite(this, node, fileId,
						localVariables, blockifiables, instrumentedLines,
						instrumentable == FORCE_INSTRUMENTATION));
			}
			
			if (node instanceof ThisExpression && !functionRewriteStack.isEmpty()) {
				rewrites.put(node, new ThisRewrite(this, node));
				functionRewriteStack.peek().useEscapedThis(true);
			}	

			if (nest) {
				currentScope = currentScope.nestScope();
			}

			if (currentScope != startScope) {
				localVariables.put(start, currentScope);
			}
		}

		private void checkForExclusion(ASTNode node) {
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

			if (node instanceof ForInStatement) {
				ForInStatement forInStatement = (ForInStatement) node;
				addToExclusionList(forInStatement.getIterationVariable());
			}
		}

		private void checkBlacklist(ASTNode node) {
		}

		private ASTNode startOfFunction(FunctionDeclaration fd) {
			Block body = fd.getBody();
			if (body == null) {
				return null;
			}
			List statements = body.statements();
			boolean useBody = statements.isEmpty();
			return (ASTNode) (useBody ? body : statements.get(0));
		}

		private ASTNode endOfFunction(FunctionDeclaration fd) {
			Block body = fd.getBody();
			if (body == null) {
				return null;
			}
			List statements = body.statements();
			boolean useBody = statements.isEmpty();
			return (ASTNode) (useBody ? body : statements
					.get(statements.size() - 1));
		}

		private Position functionPosition(ASTNode node, boolean start) {
			boolean emptyBody = node.getParent() instanceof FunctionDeclaration;

			Position pos = getPosition(node, start && !emptyBody);
			// We always use the last }, since there is no comments or
			// anything that can ruin or position.
			int offset = emptyBody ? -1 : 0;
			throw new IllegalArgumentException("!agdfgfdlj");
		}

		private boolean isAnonymous(FunctionDeclaration fd) {
			return fd.getName() == null;
		}

		@Override
		public void postVisit(ASTNode node) {
			currentPosition = getPosition(node, false);
			
			LocalVariableScope startScope = currentScope;
			boolean unnest = isNestStatement(node);

			if (node instanceof FunctionDeclaration) {
				popRedefinable();
				functionRewriteStack.pop();
			}

			if (node instanceof VariableDeclarationFragment) {
				VariableDeclarationFragment localVar = (VariableDeclarationFragment) node;
				String name = localVar.getName().getIdentifier();
				currentScope = currentScope.addLocalVariableDeclaration(name);
			}

			if (node instanceof JavaScriptUnit) {
				unit = null;
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

		private boolean isNestStatement(ASTNode node) {
			// No block scope in JS.
			return node instanceof FunctionDeclaration;
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

			Position pos = getPosition(node, before);
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

		private int isInstrumentableStatement(ASTNode node) {
			if (exclusions.contains(node)) {
				return INSTRUMENTATION_DISALLOWED;
			}

			boolean isStatement = node instanceof Statement;
			boolean isBlock = node instanceof Block;
			// We should be able to break in empty function blocks.
			boolean isEmptyFunctionBlock = isBlock
					&& ((Block) node).statements().isEmpty()
					&& node.getParent() instanceof FunctionDeclaration;
			if (isEmptyFunctionBlock) {
				return FORCE_INSTRUMENTATION;
			}
			boolean allowInstrumentation = (isStatement && !isBlock);

			return allowInstrumentation ? INSTRUMENTATION_ALLOWED
					: INSTRUMENTATION_DISALLOWED;
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

		private void block(Position position, boolean start) {
			insert(position, "\n" + (start ? '{' : '}') + "\n");
		}

		public void rewrite(long fileId, String originalSource, int fwImportLocation, Writer output,
				NavigableMap<Integer, LocalVariableScope> scopeMap,
				NavigableSet<Integer> instrumentedLines) throws IOException, CoreException {
			// TODO: I guess there are some better AST rewrite methods around.
			// Or... never mind, I've invested way too much time in this :)
			LineMap lineByLineOriginalSource = new LineMap(originalSource);

			this.originalSource = originalSource;

			for (Map.Entry<Integer, LocalVariableScope> scope : localVariables
					.entrySet()) {
				int mappedLineNo = lineByLineOriginalSource.getLine(scope
						.getKey());
				scopeMap.put(mappedLineNo, scope.getValue());
			}

			NodeRewrite rootRewrite = new NodeRewrite(this, null);
			// Collect rewrites
			for (ASTNode rewrittenNode : rewrites.keySet()) {
				NodeRewrite parentRewrite = getClosestRewriteAncestor(rewrittenNode);
				if (parentRewrite == null) {
					parentRewrite = rootRewrite;
				}
				parentRewrite.addChild(rewrites.get(rewrittenNode));
			}

			SourceRewrite doc = new SourceRewrite(originalSource);
			if (fwImportLocation > 0) {
				doc.seek(fwImportLocation);
				doc.insert(generateFrameworkImport());
			}
			rootRewrite.rewrite(null, doc);
			instrumented = doc.rewrite();

			instrumentedLines.addAll(this.instrumentedLines.keySet());

			if (output != null) {
				output.write(instrumented);
			}
		}

		private NodeRewrite getClosestRewriteAncestor(ASTNode node) {
			ASTNode parent = node.getParent();
			if (parent == null) {
				return null;
			}
			NodeRewrite parentRewrite = rewrites.get(parent);
			if (parentRewrite == null) {
				return getClosestRewriteAncestor(parent);
			}
			return parentRewrite;
		}

		public Position getPosition(ASTNode node, boolean start) {
			JavaScriptUnit unit = (JavaScriptUnit) node.getRoot();
			if (unit == null) {
				throw new IllegalStateException("Node has no matched unit");
			}
			return new Position(node, lineMap, start);
		}

		public void setScopeResetPoints(TreeSet<Integer> scopeResetPoints) {
			this.scopeResetPoints = new TreeSet<Integer>(scopeResetPoints);
		}

		private void pushRedefinable(IRedefinable redefinable, ASTNode node) {
			redefinables.put(redefinable.key(), redefinable);
			if (node != null) {
				nodeRedefinables.put(node, redefinable.key());
			}
			redefinableStack.push(redefinable);
		}

		private IRedefinable popRedefinable() {
			return redefinableStack.pop();
		}

		private IRedefinable currentRedefinable() {
			return redefinableStack.peek();
		}

		@Override
		public String getInstrumentedSource(IFilter<String> features,
				ASTNode node) {
			NodeRewrite rewrite = rewrites.get(node);
			if (rewrite == null) {
				// TODO
				throw new RuntimeException("Could not find rewrite for node.");
			}
			SourceRewrite doc = new SourceRewrite(originalSource, node);
			rewrite.rewrite(features, doc);
			return doc.rewrite();

			/*
			 * Position startPos = getPosition(node, true); Position endPos =
			 * getPosition(node, false); int start = startPos.getPosition(); int
			 * end = endPos.getPosition(); Entry<Integer, Integer>
			 * startDeltaEntry = movedSourceMap .floorEntry(start - 1);
			 * Entry<Integer, Integer> endDeltaEntry = movedSourceMap
			 * .floorEntry(end); int startDelta = startDeltaEntry == null ? 0 :
			 * startDeltaEntry .getValue(); int endDelta = endDeltaEntry == null
			 * ? 0 : endDeltaEntry.getValue(); return
			 * instrumented.substring(start + startDelta, end + endDelta);
			 */
		}

		public String getInstrumentedSource() {
			return instrumented;
		}

		public void setFileRedefinable(FileRedefinable file) {
			pushRedefinable(file, null);
		}

		@Override
		public String getSource(ASTNode node) {
			return getSource(getPosition(node, true).getPosition(),
					getPosition(node, false).getPosition());
		}

		@Override
		public String getSource(int start, int end) {
			return originalSource.substring(start, end);
		}

		@Override
		public String getSource() {
			return originalSource;
		}
		
		public void validateAST(IProblem[] problems) {
			int errorCount = 0;
			StringBuffer errorMsg = new StringBuffer();
			for (IProblem problem : problems) {
				if (problem.isError()) {
					errorMsg.append('\n' + problem.getMessage());
				}
			}
			if (errorCount > 0) {
				String truncateMsg = errorCount > ERROR_COUNT_THRESHOLD ? 
						MessageFormat.format(" (Showing the {0} first errors.)", ERROR_COUNT_THRESHOLD) : "";
				// Throw unchecked exception.
				throw new InvalidASTException( 
						MessageFormat.format("Invalid JavaScript; found {0} errors{1}:{2}", errorCount, truncateMsg, errorMsg));
			}
		}
		
		public Position getCurrentPosition() {
			return currentPosition;
		}

	}

	public static final String SERVER_HOST_PROP = "SERVER_HOST";
	public static final String SERVER_PORT_PROP = "SERVER_PORT";
	public static final String PROJECT_NAME_PROP = "PROJECT_NAME";

	private static final Map<String, IRedefinable> EMPTY = Collections
			.emptyMap();
	

	private final ASTParser parser;

	private HashMap<IPath, Long> fileIds = null;
	private final TreeMap<Long, IPath> reverseFileIds = new TreeMap<Long, IPath>();
	private final HashMap<Long, NavigableMap<Integer, LocalVariableScope>> scopeMaps = new HashMap<Long, NavigableMap<Integer, LocalVariableScope>>();
	private final HashMap<Long, NavigableSet<Integer>> lineMaps = new HashMap<Long, NavigableSet<Integer>>();
	private final HashMap<IFile, String> instrumentedSource = new HashMap<IFile, String>();
	private HashMap<IPath, Map<String, IRedefinable>> redefinables = new HashMap<IPath, Map<String, IRedefinable>>();
	private ProjectRedefinable projectRedefinable;

	private CopyOnWriteArrayList<IRedefineListener> redefineListeners = new CopyOnWriteArrayList<IRedefineListener>();

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
			projectRedefinable = null;
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
			Collection<IPath> added = diff.getAdded();
			for (IPath path : added) {
				result[0] |= (fileIds == null || fileIds.get(path) == null);
				assignFileId(path);
			}
		}
		return result[0];
	}

	public FileRedefinable delete(IPath filePath, ProjectRedefinable baseline) {
		initProjectRedefinable();
		IFile file = (IFile) ResourcesPlugin.getWorkspace().getRoot()
				.findMember(filePath);
		FileRedefinable fileRedefinable = new FileRedefinable(null, file, true);
		if (baseline != null) {
			baseline.replaceChild(fileRedefinable);
		}
		return fileRedefinable;
	}

	public void writeFramework(Writer output) throws CoreException {
		IFile frameworkFile = project.getFile(Html5Plugin
				.getHTML5Folder(project).append(getFrameworkPath()));
		String frameworkSource = generateFrameworkSource();
		this.instrumentedSource.put(frameworkFile, frameworkSource);
		if (output != null) {
			try {
				output.write(frameworkSource);
			} catch (IOException e) {
				throw new CoreException(new Status(IStatus.ERROR, Html5Plugin.PLUGIN_ID, "Cannot write debug framework", e));
			}
		}
	}
	
	public FileRedefinable rewrite(IPath filePath, Writer output,
			ProjectRedefinable baseline) throws CoreException {
		DebugRewriteOperationVisitor visitor = null;
		
		try {
			initProjectRedefinable();
			IFile file = (IFile) ResourcesPlugin.getWorkspace().getRoot()
					.findMember(filePath);
			File absoluteFile = file.getLocation().toFile();
			FileRedefinable fileRedefinable = new FileRedefinable(null, file);

			if (isValidJavaScriptFile(filePath)) {
				String source = Util.readFile(absoluteFile.getAbsolutePath());
				String prunedSource = source;
				TreeSet<Integer> scopeResetPoints = new TreeSet<Integer>();

				long fileId = assignFileId(filePath);
				LineMap sourceLineMap = new LineMap(source);
				visitor = new DebugRewriteOperationVisitor(
						sourceLineMap, fileId);

				int fwImportLocation = -1;
				if (isEmbeddedJavaScriptFile(filePath)) {
					ArrayList<Pair<Integer, Integer>> htmlRanges = new ArrayList<Pair<Integer, Integer>>();
					ArrayList<Pair<Integer, Integer>> htmlImportRanges = new ArrayList<Pair<Integer, Integer>>();
					Pair<String, Integer> prunedSourceAndLoc = getEmbeddedJavaScript(file,
							scopeResetPoints, htmlRanges);
					prunedSource = prunedSourceAndLoc.first;
					fwImportLocation = prunedSourceAndLoc.second;
					HTMLRedefinable htmlRedefinable = new HTMLRedefinable(null,
							file, visitor);
					htmlRedefinable.setHtmlRanges(htmlRanges);
					fileRedefinable = htmlRedefinable;
				}

				// 1. Parse (JSDT)
				parser.setSource(prunedSource.toCharArray());
				ASTNode ast = parser.createAST(new NullProgressMonitor());

				// 2. Instrument
				visitor.setFileRedefinable(fileRedefinable);
				visitor.setScopeResetPoints(scopeResetPoints);
				ast.accept(visitor);
				TreeMap<Integer, LocalVariableScope> scopeMap = new TreeMap<Integer, LocalVariableScope>();
				TreeSet<Integer> instrumentedLines = new TreeSet<Integer>();
				visitor.rewrite(fileId, source, fwImportLocation, output, scopeMap,
						instrumentedLines);

				// 3. Update state and notify listeners
				String instrumentedSource = visitor.getInstrumentedSource();
				this.instrumentedSource.put(file, instrumentedSource);
				scopeMaps.put(fileId, scopeMap);
				lineMaps.put(fileId, instrumentedLines);

				if (baseline != null) {
					baseline.replaceChild(fileRedefinable);
				}
			}
			return fileRedefinable;
		} catch (CoreException e) {
			throw e;
		} catch (Exception e) {
			String positionHint = "";
			Position currentPosition = null;
			if (visitor != null) {
				currentPosition = visitor.getCurrentPosition();
			}
			if (currentPosition != null) {
				positionHint = ", near line " + currentPosition.getLine();
			}
			String locationHintMsg = MessageFormat.format("In file {0}{1}: {2}", filePath.toOSString(), positionHint, e.getMessage());
			throw new CoreException(new Status(IStatus.ERROR,
					Html5Plugin.PLUGIN_ID, locationHintMsg, e));
		}
	}

	private void initProjectRedefinable() {
		// TODO: Maybe we should let all build state be stored here!?
		if (projectRedefinable == null) {
			projectRedefinable = new ProjectRedefinable(project);
		}

	}

	// Returns a string where everything that is not javascript is replaced by
	// spaces. The second value is the location of the first script tag.
	private Pair<String, Integer> getEmbeddedJavaScript(IFile file,
			NavigableSet<Integer> scopeResetPoints, ArrayList<Pair<Integer, Integer>> htmlRanges) throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		IModelManager modelManager = StructuredModelManager.getModelManager();
		IStructuredDocument doc = modelManager
				.createStructuredDocumentFor(file);
		JsTranslator translator = new JsTranslator(doc, file.getFullPath()
				.toOSString()) {
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
		org.eclipse.jface.text.Position[] htmlLocations = translator
				.getHtmlLocations();
		for (org.eclipse.jface.text.Position htmlLocation : htmlLocations) {
			scopeResetPoints.add(htmlLocation.offset);
		}
		org.eclipse.jface.text.Position[] ranges = translator
				.getHtmlLocations();
		for (int i = 0; i <= ranges.length; i++) {
			int start = i == 0 ? 0 : ranges[i - 1].offset
					+ ranges[i - 1].length;
			int end = i == ranges.length ? doc.getLength() : ranges[i].offset;
			htmlRanges.add(new Pair<Integer, Integer>(start, end));
		}
		
		// The import ranges in JSDT is (of course!) not comprehensive... so we need to roll our own.
		// Again, using internal APIs.
		IStructuredDocumentRegion[] regs = doc.getStructuredDocumentRegions();
		int fwImportLocation = -1;
		for (int i = 0; fwImportLocation == -1 && i < regs.length; i++) {
			if (regs[i] instanceof XMLStructuredDocumentRegion) {
				XMLStructuredDocumentRegion xmlReg = (XMLStructuredDocumentRegion) regs[i];
				for (int j = 0; fwImportLocation == -1 && j < xmlReg.getRegions().size(); j++) {
					if (xmlReg.getRegions().get(j) instanceof TagNameRegion) {
						TagNameRegion tag = (TagNameRegion) xmlReg.getRegions().get(j);
						String tagName = xmlReg.getFullText(tag);
						if ("script".equalsIgnoreCase(tagName.trim())) {
							fwImportLocation = xmlReg.getStart();
							continue;
						}
					}
				}
			}	
		}
		
		/*org.eclipse.jface.text.Position[] importRanges = translator.getImportHtmlRanges();
		for (int i = 0; i < importRanges.length; i++) {
			org.eclipse.jface.text.Position importRange = importRanges[i];
			importHtmlRanges.add(new Pair<Integer, Integer>(importRange.offset, importRange.offset + importRange.length));
		}*/

		return new Pair<String, Integer>(translator.getJsText(), fwImportLocation);
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
				// TODO: Force build instead!!!?
				rewrite(file.getFullPath(), null, getBaseline());
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

	public String generateFrameworkImport() throws CoreException {
		return MessageFormat.format(
				"<script type=\"text/javascript\" charset=\"utf-8\" src=\"{0}\"></script>",
				getFrameworkPath());
	}
	
	public static String getFrameworkPath() {
		return "wormhole_dbg_fw.js";
	}

	public String generateFrameworkSource() throws CoreException {
		MoSyncProject project = MoSyncProject.create(this.project);
		StringWriter boilerplateOutput = new StringWriter();
		writeTemplate(project, "/templates/jsoddsupport.template", new HashMap<String, String>(),
				boilerplateOutput);
		return boilerplateOutput.getBuffer().toString();
	}

	public void generateRemoteFetch(MoSyncProject project, IResource resource,
			Writer remoteFetchOutput) throws CoreException {
		Map<String, String> additionalProperties = new HashMap<String, String>();
		if (resource instanceof IFile) {
			additionalProperties.put("FILE_PATH", Html5Plugin.getDefault().getLocalPath((IFile) resource).toPortableString());
			if (isEmbeddedJavaScriptFile(resource.getFullPath())) {
				writeTemplate(project, "/templates/hcr.template", additionalProperties, remoteFetchOutput);	
			} else {
				writeTemplate(project, "/templates/hcrjs.template", additionalProperties, remoteFetchOutput);
			}
			
		}
	}

	private void writeTemplate(MoSyncProject project, String templatePath, Map<String, String> additionalProperties,
			Writer output) throws CoreException {
		Template template = new Template(getClass().getResource(templatePath));
		Map<String, String> properties = getTemplateProperties(project);
		properties.putAll(additionalProperties);
		try {
			String contents = template.resolve(properties);
			output.write(contents);
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Html5Plugin.PLUGIN_ID, e.getMessage(), e));
		}
	}

	private Map<String, String> getTemplateProperties(MoSyncProject project)
			throws CoreException {
		Map<String, String> properties = getDefaultProperties();
		properties.putAll(project.getProperties());

		properties.put("INIT_FILE_IDS", generateFileIdInitCode());
		properties.put("PROJECT_NAME", project.getName());
		properties.put("TIMEOUT_IN_MS",
				Integer.toString(1000 * Html5Plugin.getDefault().getTimeout()));
		return properties;
	}

	public static Map<String, String> getDefaultProperties() throws CoreException {
		URL serverURL;
		try {
			serverURL = Html5Plugin.getDefault().getServerURL();
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Html5Plugin.PLUGIN_ID,
					"Could not determine localhost address"));
		}

		HashMap<String, String> properties = new HashMap<String, String>();
		
		properties.put(SERVER_HOST_PROP, serverURL.getHost());
		properties.put(SERVER_PORT_PROP, Integer.toString(serverURL.getPort()));
		
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

	/**
	 * Returns the best matching breakpoint for a specific file/line pair.
	 * 
	 * @param path
	 * @param hitLine
	 * @return
	 */
	public static IJavaScriptLineBreakpoint findBreakPoint(IPath path, int hitLine) {
		IBreakpoint[] bps = DebugPlugin.getDefault().getBreakpointManager()
				.getBreakpoints(JavaScriptDebugModel.MODEL_ID);
		IJavaScriptLineBreakpoint closest = null;
		for (IBreakpoint bp : bps) {
			if (bp instanceof IJavaScriptLineBreakpoint) {
				IJavaScriptLineBreakpoint lineBp = (IJavaScriptLineBreakpoint) bp;
				try {
					if (Util.equals(path, new Path(lineBp.getScriptPath()))) {
						int closestLine = closest == null ? 0 : closest
								.getLineNumber();
						int bpLine = lineBp.getLineNumber();
						// We will try to get as close as possible to the hit
						// line
						// but never *after* it.
						if (bpLine > closestLine && bpLine <= hitLine) {
							closest = (IJavaScriptLineBreakpoint) bp;
						}
					}
				} catch (CoreException e) {
					// Just IGNORE!
				}
			}
		}
		return closest;
	}

	/**
	 * <p>
	 * Not all lines are actually instrumented for line breakpoints. This method
	 * will find the best matching line that has been instrumented for
	 * breakpoints.
	 * </p>
	 * <p>
	 * The best matching line will always be at or <b>after</b> the given line.
	 * </p>
	 * 
	 * @param file
	 * @param line
	 * @return {@code -1} if no match is found.
	 */
	public int findClosestBreakpointLine(IPath file, int line) {
		if (line < 0) {
			return line;
		}
		Long fileId = fileIds.get(file);
		if (fileId != null) {
			NavigableSet<Integer> lineMap = lineMaps.get(fileId);
			Integer bestMatch = lineMap == null ? null : lineMap.ceiling(line);
			if (bestMatch != null) {
				return bestMatch;
			}
		}
		return -1;
	}

	public boolean requiresFullBuild() {
		return projectRedefinable == null;
	}

	public ProjectRedefinable getBaseline() {
		initProjectRedefinable();
		return projectRedefinable;
	}

}
