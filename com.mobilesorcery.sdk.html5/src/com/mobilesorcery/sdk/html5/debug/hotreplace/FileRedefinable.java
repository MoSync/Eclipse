package com.mobilesorcery.sdk.html5.debug.hotreplace;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.wst.jsdt.core.dom.ASTNode;

import com.mobilesorcery.sdk.core.IProvider;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.IRedefinable;
import com.mobilesorcery.sdk.html5.debug.RedefineException;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadVirtualMachine;

public class FileRedefinable extends AbstractRedefinable {

	private IFile file;

	public FileRedefinable(IRedefinable parent,
			IProvider<String, ASTNode> source, IFile file) {
		super(parent, source);
		this.file = file;
	}

	@Override
	public String key() {
		return file.getFullPath().toPortableString();
	}

	@Override
	public void redefine(IRedefinable toBeRedefined, boolean inPlace)
			throws RedefineException {
		if (!inPlace) {
			List<ReloadVirtualMachine> vms = Html5Plugin.getDefault().getReloadServer().getVMs(false);
			for (ReloadVirtualMachine vm : vms) {
				if (Util.equals(vm.getProject(), file.getProject())) {
					vm.update(file, false);
				}
			}
		} else {
			throw new RedefineException("Files are never redefined in-place");
		}
	}
}
