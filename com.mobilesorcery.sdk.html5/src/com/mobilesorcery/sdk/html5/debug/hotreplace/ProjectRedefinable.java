package com.mobilesorcery.sdk.html5.debug.hotreplace;

import org.eclipse.core.resources.IProject;

public class ProjectRedefinable extends AbstractRedefinable {

	private IProject project;

	public ProjectRedefinable(IProject project) {
		super(null, null);
		this.project = project;
	}

	@Override
	public String key() {
		return constructKey(getProject().getName());
	}
	
	public IProject getProject() {
		return project;
	}

}
