package com.mobilesorcery.sdk.html5.debug;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.ILocalVariable;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTParser;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.SwitchCase;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.jsdt.internal.core.LocalVariable;

import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.templates.Template;

public class JSODDSupport {

	public class DebugRewriteOperationVisitor extends ASTVisitor {
		private final HashMap<Integer, Integer> linesToRewrite = new HashMap<Integer, Integer>();
		private JavaScriptUnit unit;
		private final Stack<ASTNode> statementStack = new Stack<ASTNode>();
		private boolean allowInstrumentation = false;
		private LocalVariableScope currentScope = new LocalVariableScope().nestScope();
		private final TreeMap<Integer, LocalVariableScope> localVariables = new TreeMap<Integer, LocalVariableScope>();

		@Override
		public void preVisit(ASTNode node) {
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
				allowInstrumentation = true;
				nest = true;
			}

			if (isInstrumentableStatement(node) && unit != null) {
				addInstrumentationLocation(line, col);
			}

			if (nest) {
				currentScope = currentScope.nestScope();
			}

			if (currentScope != startScope) {
				// It's ok to overwrite the previous localvariables if on the same line.
				localVariables.put(line, currentScope);
			}
		}

		private void addInstrumentationLocation(int line, int col) {
			if (line > 0 && col >= 0 && !linesToRewrite.containsKey(line)) {
				linesToRewrite.put(line, col);
			}
		}

		private boolean isInstrumentableStatement(ASTNode node) {
			return allowInstrumentation && node instanceof Statement
					&& !(node instanceof Block) && !(node instanceof SwitchCase);
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
				//allowInstrumentation = false;
				unnest = true;
			}

			if (unnest) {
				currentScope = currentScope.unnestScope();
			}

			if (currentScope != startScope) {
				// It's ok to overwrite the previous localvariables if on the same line.
				localVariables.put(line, currentScope);
			}
		}

		public void rewrite(long fileId, String source, Writer output, NavigableMap<Integer, LocalVariableScope> scopeMap) throws IOException {
			// TODO: I guess there is some better AST rewrite methods around.
			// TODO: Blockless statements that now should be blocks, such as single line 'if's
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
					Entry<Integer, LocalVariableScope> scope = localVariables.floorEntry(lineNo);
					String scopeDesc = "";
					if (scope != null) {
						scopeDesc = "/*" + scope.getValue().getLocalVariables() + "*/";
					}
					// TODO: Faster check here!
					String prefixAndNewLine = prefix.trim().length() == 0 ? "" : (prefix + "\n");
					newLine = prefixAndNewLine +
						scopeDesc + " MoSyncDebugProtocol.updatePosition(" + fileId + "," + lineNo + "," + "false,function(____eval) {return eval(____eval);});" +
						"\n" + suffix;
				}

				result.append(newLine);
				result.append('\n');
				lineNo++;
			}
			output.write(result.toString());
		}
	}

	private ASTParser parser;

	private final HashMap<IPath, Long> fileIds = new HashMap<IPath, Long>();
	private final HashMap<Long, IPath> reverseFileIds = new HashMap<Long, IPath>();
	private final HashMap<Long, NavigableMap<Integer, LocalVariableScope>> scopeMaps = new HashMap<Long, NavigableMap<Integer, LocalVariableScope>>();
	private long currentFileId = 0;

	public void init() {
		parser = ASTParser.newParser(AST.JLS3);
	}

	public void rewrite(IPath file, Writer output) throws IOException {
		if (!isValidJavaScriptFile(file)) {
			return;
		}
		init();
		File absoluteFile = ResourcesPlugin.getWorkspace().getRoot().findMember(file).getLocation().toFile();
		String source = Util.readFile(absoluteFile.getAbsolutePath());
		parser.setSource(source.toCharArray());
		ASTNode ast = parser.createAST(new NullProgressMonitor());
		DebugRewriteOperationVisitor visitor = new DebugRewriteOperationVisitor();
		ast.accept(visitor);
		long fileId = getFileId(file);
		TreeMap<Integer, LocalVariableScope> scopeMap = new TreeMap<Integer, LocalVariableScope>();
		visitor.rewrite(fileId, source, output, scopeMap);
		scopeMaps.put(fileId, scopeMap);
	}

	public static boolean isValidJavaScriptFile(IPath file) {
		// TODO: Use content descriptors!?
		return file != null && ("js".equalsIgnoreCase(file.getFileExtension()) ||
		 "html".equalsIgnoreCase(file.getFileExtension()));
	}

	public long getFileId(IPath file) {
		Long fileId = fileIds.get(file);
		if (fileId == null) {
			fileId = currentFileId;
			fileIds.put(file, fileId);
			reverseFileIds.put(fileId, file);
			currentFileId++;
		}

		return fileId;
	}

	public IPath getFile(long fileId) {
		return reverseFileIds.get(fileId);
	}

	public LocalVariableScope getScope(long fileId, int lineNo) {
		NavigableMap<Integer, LocalVariableScope> scopeMap = scopeMaps.get(fileId);
		if (scopeMap != null) {
			Entry<Integer, LocalVariableScope> scope = scopeMap.floorEntry(lineNo);
			if (scope != null) {
				return scope.getValue();
			}
		}

		return LocalVariableScope.EMPTY;
	}

	public void generateBoilerplate(IPropertyOwner projectProperties, Writer boilerplateOutput) throws IOException {
		Template template = new Template(getClass().getResource("/templates/jsoddsupport.template"));
		// TODO! Use Reload instead!
		HashMap<String, String> properties = new HashMap<String, String>(projectProperties.getProperties());
		if (!properties.containsKey("SERVER_HOST")) {
			properties.put("SERVER_HOST", "192.168.1.64");
		}
		if (!properties.containsKey("SERVER_PORT")) {
			properties.put("SERVER_PORT", "8511");
		}
		String contents = template.resolve(properties);
		boilerplateOutput.write(contents);
	}
}
