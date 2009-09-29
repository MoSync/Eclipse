package com.mobilesorcery.sdk.ui.internal.properties;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.BuildAction;

public class ProjectBuildAction extends BuildAction {

	private IProject project;

	public ProjectBuildAction(Shell shell, int type) {
		super(shell, type);
	}

	public void setProject(IProject project) {
		this.project = project;
	}
	
	public List getActionResources() {
		return Arrays.asList(project);
	}
	
	public List getSelectedResources()  {
		return getActionResources();
	}
}
