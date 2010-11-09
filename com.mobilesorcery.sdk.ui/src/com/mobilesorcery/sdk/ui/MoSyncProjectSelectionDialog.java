/*  Copyright (C) 2010 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.ui;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncNature;

public class MoSyncProjectSelectionDialog extends ElementListSelectionDialog {

	public MoSyncProjectSelectionDialog(Shell parent) {
		super(parent, getRenderer());
		setTitle("Select MoSync Project");
		setMessage("Select [an open] MoSync Project for this launch");
	}

	public void setInitialProject(IProject project) {
		if (project != null) {
			setInitialSelections(new Object[] { project });
		}
	}

	/**
	 * Opens the dialog and returns the selected project, or <code>null</code>
	 * if cancelled.
	 * 
	 * @return
	 */
	public IProject selectProject() {
		try {
			setElements(getMoSyncProjects(ResourcesPlugin.getWorkspace()
					.getRoot()));
		} catch (CoreException e) {
			CoreMoSyncPlugin.getDefault().log(e);
		}

		if (open() == Window.OK) {
			return (IProject) getFirstResult();
		}

		return null;
	}

	private IProject[] getMoSyncProjects(IWorkspaceRoot root)
			throws CoreException {
		IProject[] allProjects = root.getProjects();
		ArrayList<IProject> result = new ArrayList<IProject>();
		for (int i = 0; i < allProjects.length; i++) {
			if (allProjects[i].isOpen()
					&& allProjects[i].hasNature(MoSyncNature.ID)) {
				result.add(allProjects[i]);
			}
		}

		return result.toArray(new IProject[0]);
	}

	private static ILabelProvider getRenderer() {
		return WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider();
		/*ILabelProvider labelProvider = new LabelProvider() {
			public String getText(Object o) {
				if (o instanceof IProject) {
					return ((IProject) o).getName();
				}

				return "?";
			}
		};

		return labelProvider;*/
	}
}
