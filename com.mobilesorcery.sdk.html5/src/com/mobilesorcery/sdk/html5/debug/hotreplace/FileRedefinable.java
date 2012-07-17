package com.mobilesorcery.sdk.html5.debug.hotreplace;

import java.util.HashSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;

import com.mobilesorcery.sdk.html5.debug.IRedefinable;

public class FileRedefinable extends AbstractRedefinable {

	private IFile file;
	private boolean deleted;

	public FileRedefinable(IRedefinable parent, IFile file) {
		this(parent, file, false);
	}
	
	public FileRedefinable(IRedefinable parent, IFile file, boolean deleted) {
		super(parent, null);
		this.file = file;
		this.deleted = deleted;
	}

	public IFile getFile() {
		return file;
	}
	
	public boolean isDeleted() {
		return deleted;
	}
	
	@Override
	public String key() {
		return constructKey(file.getProjectRelativePath().toPortableString());
	}

	public int getMemSize() {
		int result = 0;
		HashSet<ASTNode> roots = new HashSet<ASTNode>();
		// A simple appx of memory consumption
		for (IRedefinable redefinable : getChildren()) {
			if (redefinable instanceof ASTRedefinable) {
				ASTRedefinable astRedefinable = (ASTRedefinable) redefinable;
				ASTNode node = astRedefinable.getNode();
				ASTNode root = node == null ? null : node.getRoot();
				if (root instanceof JavaScriptUnit && !roots.contains(root)) {
					result += root.subtreeBytes();
					roots.add(root);
				}
			}
		}
		return result;
	}

	public String toString() {
		return file.getFullPath().toPortableString();
	}
}
