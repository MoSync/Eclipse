/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
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
