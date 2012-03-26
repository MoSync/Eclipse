package com.mobilesorcery.sdk.html5.debug;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Stack;
import java.util.TreeMap;

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
import org.eclipse.wst.jsdt.core.dom.ForInStatement;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.SwitchCase;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.templates.Template;
import com.mobilesorcery.sdk.html5.Html5Plugin;

public class JSODDSupport {

	public class DebugRewriteOperationVisitor extends ASTVisitor {
		private final HashMap<Integer, Integer> linesToRewrite = new HashMap<Integer, Integer>();
		private JavaScriptUnit unit;
		private final Stack<ASTNode> statementStack = new Stack<ASTNode>();
		private LocalVariableScope currentScope = new LocalVariableScope()
				.nestScope();
		private final TreeMap<Integer, LocalVariableScope> localVariables = new TreeMap<Integer, LocalVariableScope>();

		@Override
		public void preVisit(ASTNode node) {
			System.err.println(Util.fill(' ', statementStack.size() * 2) + node.getClass() + ": " + node.toString().replace('\n', ' '));
			int start = node.getStartPosition();
			int line = unit == null ? -1 : unit.getLineNumber(start);
			int col = unit == null ? -1 : unit.getColumnNumber(start);

			LocalVariableScope startScope = currentScope;
			boolean nest = false;

			if (node instanceof FunctionDeclaration) {
				nest = true;
			}

			if (node instanceof JavaScriptUnit) {
				unit = (JavaScriptUnit) node;
			}

			if (node instanceof Statement) {
				statementStack.push(node);
			}

			if (node instanceof Block) {
				nest = true;
			}

			if (isInstrumentableStatement(node) && unit != null) {
				addInstrumentationLocation(line, col);
			}

			if (nest) {
				currentScope = currentScope.nestScope();
			}

			if (currentScope != startScope) {
				// It's ok to overwrite the previous localvariables if on the
				// same line.
				localVariables.put(line, currentScope);
			}
		}

		private void addInstrumentationLocation(int line, int col) {
			if (line > 0 && col >= 0 && !linesToRewrite.containsKey(line)) {
				linesToRewrite.put(line, col);
			}
		}

		private boolean isInstrumentableStatement(ASTNode node) {
			boolean isStatement = node instanceof Statement;
			boolean isBlock = node instanceof Block;
			ASTNode parent = node.getParent();
			boolean allowInstrumentation = isStatement && !isBlock;

			// Special case: switch.
			allowInstrumentation &= !(node instanceof SwitchCase);

			// Special case: for
			if (parent instanceof ForInStatement) {
				ForInStatement forInStatement = (ForInStatement) parent;
				allowInstrumentation &= node == forInStatement.getBodyChild();
			}

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

			if (node instanceof Statement) {
				statementStack.pop();
			}
			if (node instanceof Block) {
				unnest = true;
			}

			if (unnest) {
				currentScope = currentScope.unnestScope();
			}

			if (currentScope != startScope) {
				// It's ok to overwrite the previous localvariables if on the
				// same line.
				localVariables.put(line, currentScope);
			}
		}

		public void rewrite(long fileId, String source, Writer output,
				NavigableMap<Integer, LocalVariableScope> scopeMap)
				throws IOException {
			// TODO: I guess there is some better AST rewrite methods around.
			// TODO: Blockless statements that now should be blocks, such as
			// single line 'if's
			String[] lineByLineSource = source.split("\\n");
			StringBuffer result = new StringBuffer();
			int lineNo = 1;
			for (String line : lineByLineSource) {
				Integer col = linesToRewrite.get(lineNo);
				String newLine = line;
				if (col != null && col >= 0) {
					col = Math.min(col, line.length());
					String prefix = line.substring(0, col);
					String suffix = line.substring(col);
					Entry<Integer, LocalVariableScope> scope = localVariables
							.floorEntry(lineNo);
					String scopeDesc = "";
					if (scope != null) {
						scopeDesc = "/*" + scope.getValue().getLocalVariables()
								+ "*/";
					}
					// TODO: Faster check here!
					String prefixAndNewLine = prefix.trim().length() == 0 ? ""
							: (prefix + "\n");
					newLine = prefixAndNewLine
							+ scopeDesc
							+ " MoSyncDebugProtocol.updatePosition("
							+ fileId
							+ ","
							+ lineNo
							+ ","
							+ "false,function(____eval) {return eval(____eval);});"
							+ "\n" + suffix;
				}

				result.append(newLine);
				result.append('\n');
				lineNo++;
			}
			if (output != null) {
				output.write(result.toString());
			}
		}
	}

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
		return file != null
				&& ("js".equalsIgnoreCase(file.getFileExtension()) || "html"
						.equalsIgnoreCase(file.getFileExtension()));
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
		HashMap<String, String> properties = new HashMap<String, String>(
				projectProperties.getProperties());
		if (!properties.containsKey("SERVER_HOST")) {
			try {
				InetAddress localHost = InetAddress.getLocalHost();
				properties.put("SERVER_HOST", localHost.getHostAddress());
			} catch (IOException e) {
				throw new CoreException(new Status(IStatus.ERROR, Html5Plugin.PLUGIN_ID, "Could not determine localhost address"));
			}
		}
		if (!properties.containsKey("SERVER_PORT")) {
			properties.put("SERVER_PORT", "8511");
		}
		properties.put("INIT_FILE_IDS", generateFileIdInitCode());
		try {
			String contents = template.resolve(properties);
			boilerplateOutput.write(contents);
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Html5Plugin.PLUGIN_ID, e.getMessage(), e));
		}
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
