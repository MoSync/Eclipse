package com.mobilesorcery.sdk.builder.blackberry;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class Simulator {

	private Path path;

	public Simulator(Path path) {
		this.path = path;
	}
	
	public IPath getPath() {
		return path;
	}
}
