package com.mobilesorcery.sdk.html5.debug.jsdt;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.jsdt.debug.core.jsdi.Location;
import org.eclipse.wst.jsdt.debug.core.jsdi.ScriptReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;

public class SimpleScriptReference implements ScriptReference {

	private final ReloadVirtualMachine vm;
	private final IFile file;

	public SimpleScriptReference(ReloadVirtualMachine vm, IFile file) {
		this.vm = vm;
		this.file = file;
	}

	@Override
	public VirtualMachine virtualMachine() {
		return vm;
	}

	@Override
	public List allLineLocations() {
		return Collections.EMPTY_LIST;
	}

	@Override
	public Location lineLocation(int lineNumber) {
		return new SimpleLocation(vm, file, lineNumber);
	}

	@Override
	public List allFunctionLocations() {
		return null;
	}

	@Override
	public Location functionLocation(String functionName) {
		return null;
	}

	@Override
	public String source() {
		return null;
	}

	@Override
	public URI sourceURI() {
		return file.getLocationURI();
	}
	
	public IPath sourcePath() {
		return file.getLocation();
	}
	
	public IFile getFile() {
		return file;
	}

	public static IPath getFile(Location location) {
		ScriptReference ref = location.scriptReference();
		if (ref instanceof SimpleScriptReference) {
			return ((SimpleScriptReference) ref).sourcePath();
		}
		return null;
	}

}
