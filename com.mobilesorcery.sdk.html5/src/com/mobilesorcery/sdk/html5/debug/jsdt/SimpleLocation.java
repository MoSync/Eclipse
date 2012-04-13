package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.jsdt.debug.core.jsdi.Location;
import org.eclipse.wst.jsdt.debug.core.jsdi.ScriptReference;

public class SimpleLocation implements Location {

	private final SimpleScriptReference ref;
	private final int line;
	private String functionName;

	public SimpleLocation(ReloadVirtualMachine vm, IFile file, int line) {
		this.ref = new SimpleScriptReference(vm, file);
		this.line = line;
	}

	public void setFunctionName(String functionName) {
		this.functionName = functionName;
	}

	@Override
	public ScriptReference scriptReference() {
		return ref;
	}

	@Override
	public int lineNumber() {
		return line;
	}

	@Override
	public String functionName() {
		return functionName;
	}

}
