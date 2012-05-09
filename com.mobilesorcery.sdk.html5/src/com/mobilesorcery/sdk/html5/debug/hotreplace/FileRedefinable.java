package com.mobilesorcery.sdk.html5.debug.hotreplace;

import org.eclipse.core.resources.IFile;
import org.eclipse.wst.jsdt.core.dom.ASTNode;

import com.mobilesorcery.sdk.core.IProvider;
import com.mobilesorcery.sdk.html5.debug.IRedefinable;

public class FileRedefinable extends AbstractRedefinable {

	private IFile file;

	public FileRedefinable(IRedefinable parent, IFile file) {
		super(parent, null);
		this.file = file;
	}

	public IFile getFile() {
		return file;
	}
	
	@Override
	public String key() {
		return constructKey(file.getProjectRelativePath().toPortableString());
	}

}
