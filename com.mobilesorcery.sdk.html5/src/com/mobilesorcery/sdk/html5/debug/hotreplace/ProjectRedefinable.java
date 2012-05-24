package com.mobilesorcery.sdk.html5.debug.hotreplace;

import java.util.List;

import org.eclipse.core.resources.IProject;

import com.mobilesorcery.sdk.html5.debug.IRedefinable;

public class ProjectRedefinable extends AbstractRedefinable {

	private IProject project;

	public ProjectRedefinable(IProject project) {
		super(null, null);
		this.project = project;
	}

	public ProjectRedefinable shallowCopy() {
		// NOTE! Do not modify the file redefinables...
		ProjectRedefinable result = new ProjectRedefinable(project);
		result.addChildren(getChildren());
		return result;
	}
	
	private void addChildren(List<IRedefinable> children) {
		for (IRedefinable child : children) {
			addChild(child);
		}
	}

	@Override
	public String key() {
		return constructKey(getProject().getName());
	}
	
	public IProject getProject() {
		return project;
	}


}
