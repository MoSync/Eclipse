package com.mobilesorcery.sdk.html5.debug.jsdt;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.jsdt.debug.core.jsdi.Location;
import org.eclipse.wst.jsdt.debug.core.jsdi.ScriptReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public class SimpleScriptReference implements ScriptReference {

	private final ReloadVirtualMachine vm;
	private final IPath path;

	public SimpleScriptReference(ReloadVirtualMachine vm, IFile file) {
		this(vm, file.getFullPath());
	}

	public SimpleScriptReference(ReloadVirtualMachine vm, IPath path) {
		this.vm = vm;
		this.path = path;
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
		return new SimpleLocation(vm, path, lineNumber);
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
		return path.toFile().toURI();
	}

	public IPath sourcePath() {
		return getFile().getLocation();
	}

	public IFile getFile() {
		return ResourcesPlugin.getWorkspace().getRoot().getFile(path);
	}

	public static IPath getFile(Location location) {
		ScriptReference ref = location.scriptReference();
		if (ref instanceof SimpleScriptReference) {
			return ((SimpleScriptReference) ref).sourcePath();
		}
		return null;
	}

}
