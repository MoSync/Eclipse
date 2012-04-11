package com.mobilesorcery.sdk.html5.debug;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.eclipse.wst.jsdt.core.dom.DoStatement;
import org.eclipse.wst.jsdt.core.dom.ForInStatement;
import org.eclipse.wst.jsdt.core.dom.ForStatement;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.IfStatement;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.LabeledStatement;
import org.eclipse.wst.jsdt.core.dom.SingleVariableDeclaration;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.SwitchCase;
import org.eclipse.wst.jsdt.core.dom.SwitchStatement;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.core.dom.WhileStatement;
import org.eclipse.wst.jsdt.core.dom.WithStatement;

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
		private final HashMap<Integer, ASTNode> nodesToRewrite = new HashMap<Integer, ASTNode>();
		private JavaScriptUnit unit;
		private final Stack<ASTNode> statementStack = new Stack<ASTNode>();
		private final Stack<Boolean> instrumentState = new Stack<Boolean>();
		private LocalVariableScope currentScope = new LocalVariableScope().nestScope();
		private final TreeMap<Integer, LocalVariableScope> localVariables = new TreeMap<Integer, LocalVariableScope>();
		private final HashSet<ASTNode> exclusions = new HashSet<ASTNode>();
		private final HashSet<Statement> blockifiables = new HashSet<Statement>();
		private final HashMap<Integer, NavigableMap<Integer, List<String>>> insertions = new HashMap<Integer, NavigableMap<Integer,List<String>>>();
		private final HashMap<ASTNode, JavaScriptUnit> nodeToUnitMap = new HashMap<ASTNode, JavaScriptUnit>();

		@Override
		public void preVisit(ASTNode node) {
			//System.err.println(Util.fill(' ', statementStack.size() * 2) + node.getClass() + ": " + node.toString().replace('\n', ' '));
			int start = node.getStartPosition();
			int startLine = unit == null ? -1 : unit.getLineNumber(start);

			LocalVariableScope startScope = currentScope;
			boolean nest = false;

			blockify(node);

			if (node instanceof FunctionDeclaration) {
				FunctionDeclaration fd = (FunctionDeclaration) node;
				currentScope = currentScope.nestScope();
				for (Object paramObj : fd.parameters()) {
					SingleVariableDeclaration param = (SingleVariableDeclaration) paramObj;
					String name = param.getName().getIdentifier();
					currentScope = currentScope.addLocalVariableDeclaration(name);
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

			if (node instanceof Block) {
				// TODO: For statements etc should be able to handle one-liners!
				instrumentState.push(true);
				nest = true;
			}

			if (isInstrumentableStatement(node)) {
				addInstrumentationLocation(unit, node);
			}

			if (nest) {
				currentScope = currentScope.nestScope();
			}

			if (currentScope != startScope) {
				// It's ok to overwrite the previous localvariables if on the
				// same line.
				localVariables.put(startLine + 1, currentScope);
			}
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

		private void addInstrumentationLocation(JavaScriptUnit unit, ASTNode node) {
			if (unit == null) {
				return;
			}
			int startLine = unit.getLineNumber(node.getStartPosition());
			int startCol = unit.getColumnNumber(node.getStartPosition());
			ASTNode previousNode = nodesToRewrite.get(startLine);
			int previousStartCol = previousNode == null ? Integer.MAX_VALUE : unit.getColumnNumber(previousNode.getStartPosition());
			// Max one point of instrumentation per line.
			if (startLine > 0 && startCol >= 0 && previousStartCol > startCol) {
				nodesToRewrite.put(startLine, node);
				nodeToUnitMap.put(node, unit);
			}
		}

		private boolean isInstrumentableStatement(ASTNode node) {
			if (exclusions.contains(node)) {
				return false;
			}
			boolean isStatement = node instanceof Statement;
			boolean isBlock = node instanceof Block;
			boolean allowInstrumentation = isStatement && !isBlock;

			// Special case: switch.
			//allowInstrumentation &= !(node instanceof SwitchCase);

			allowInstrumentation &= instrumentState.isEmpty() || instrumentState.peek();

			return allowInstrumentation;
		}

		@Override
		public void postVisit(ASTNode node) {
			int start = node.getStartPosition();
			int line = unit == null ? -1 : unit.getLineNumber(start);

			LocalVariableScope startScope = currentScope;
			boolean unnest = false;

			if (node instanceof VariableDeclarationFragment) {
				VariableDeclarationFragment localVar = (VariableDeclarationFragment) node;
				String name = localVar.getName().getIdentifier();
				currentScope = currentScope.addLocalVariableDeclaration(name);
			}

			if (node instanceof JavaScriptUnit) {
				unit = null;
			}

			if (node instanceof FunctionDeclaration) {
				unnest = true;
			}

			if (node instanceof ForInStatement || node instanceof ForStatement) {
				//instrumentState.pop();
			}

			if (node instanceof Statement) {
				statementStack.pop();
			}

			if (node instanceof Block) {
				instrumentState.pop();
				unnest = true;
			}

			if (unnest) {
				currentScope = currentScope.unnestScope();
			}

			if (currentScope != startScope) {
				// It's ok to overwrite the previous localvariables if on the
				// same line.
				localVariables.put(line + 1, currentScope);
			}

			exclusions.remove(node);
		}

		private void insert(int line, int col, String text) {
			NavigableMap<Integer, List<String>> insertionsForLine = insertions.get(line);
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
				ASTNode node = nodesToRewrite.get(lineNo);
				JavaScriptUnit unit = nodeToUnitMap.get(node);
				Integer col = (unit != null && node != null) ? unit.getColumnNumber(node.getStartPosition()) : null;
				if (node != null && col != null) {
					col = Math.min(col, line.length());

					// Makes it more legible for debugging purposes
					if (onlyWS(line.substring(0, col))) {
						col = 0;
					}

					if (shouldBlockify(node)) {
						insert(lineNo, col, "{");
						int endPosition = node.getStartPosition() + node.getLength();
						insert(unit.getLineNumber(endPosition), unit.getColumnNumber(endPosition), "}");
					}

					Entry<Integer, LocalVariableScope> scope = localVariables.floorEntry(lineNo);
					String scopeDesc = "";
					if (scope != null) {
						scopeDesc = "/*" + scope.getValue().getLocalVariables()
								+ "*/";
					}

					String addThis = scopeDesc
							+ " MoSyncDebugProtocol.updatePosition("
							+ fileId
							+ ","
							+ lineNo
							+ ","
							+ "false,function(____eval) {return eval(____eval);});"
							+ "\n";
					insert(lineNo, col, addThis);
				}
				lineNo++;
			}

			scopeMap.putAll(localVariables);

			lineNo = 1;
			for (String originalLine : lineByLineSource) {
				NavigableMap<Integer, List<String>> insertionList = insertions.get(lineNo);
				String newLine = originalLine;
				if (insertionList != null) {
					ArrayList<String> segments = new ArrayList<String>();
					int originalCol = 0;
					for (Integer col : insertionList.keySet()) {
						List<String> snippets = insertionList.get(col);
						segments.add(originalLine.substring(originalCol, col));
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

		private boolean onlyWS(String str) {
			for (int i = 0; i < str.length(); i++) {
				char ch = str.charAt(i);
				if (!Character.isWhitespace(ch)) {
					return false;
				}
			}
			return true;
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
						if (MoSyncBuilder.isInOutput(resource.getProject(), resource)) {
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
		return file != null
				&& "js".equalsIgnoreCase(file.getFileExtension());
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
					.floorEntry(lineNo);
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
			throw new CoreException(new Status(IStatus.ERROR, Html5Plugin.PLUGIN_ID, "Could not determine localhost address"));
		}
		properties.put(SERVER_PORT_PROP, "8511");
		return properties;
	}

	private String generateFileIdInitCode() {
		StringBuffer result = new StringBuffer();
		for (Map.Entry<Long, IPath> entry : reverseFileIds.entrySet()) {
			result.append("idToFile[" + entry.getKey() + "]=\""
					+ entry.getValue().toPortableString() + "\";\n");
			result.append("fileToId[\"" + entry.getValue().toPortableString() + "\"]="
					+ entry.getKey() + ";\n");
		}
		return result.toString();
	}
}
