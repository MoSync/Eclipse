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
package com.mobilesorcery.sdk.product.intro;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.statushandlers.StatusManager;

import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.importproject.Activator;
import com.mobilesorcery.sdk.importproject.ImportProjectsRunnable;


public class ImportExampleProject {

	private String projectName;
	private String projectExtension;
	private String projectExtraDir;
	private String openInEditors;
	
	public ImportExampleProject(String project, String extension, String extradir, String openInEditor) {
		projectName = project;
		projectExtension = extension;
		projectExtraDir = extradir;
		this.openInEditors = openInEditor;
	}
	
	public boolean doImport() {

		String projectString = Messages.ImportExampleProject_0;
		boolean success = false;
		if (MoSyncTool.getDefault().isValid()) {
			if (projectExtraDir.equalsIgnoreCase("")) { //$NON-NLS-1$
				projectString = MoSyncTool.getDefault().getMoSyncHome().append("examples").append(projectName).append(projectName + "." + projectExtension).toOSString(); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				projectString = MoSyncTool.getDefault().getMoSyncHome().append("examples").append(projectExtraDir).append(projectName).append(projectName + "." + projectExtension).toOSString(); //$NON-NLS-1$ //$NON-NLS-2$
			}
			File[] fileChildren = new File[1];
			fileChildren[0] = new File(projectString);
			if (fileChildren[0].exists()) {
				List<IProject> result = new ArrayList<IProject>();
				ImportProjectsRunnable importproject = new ImportProjectsRunnable(fileChildren, ImportProjectsRunnable.COPY_ALL_FILES | ImportProjectsRunnable.USE_NEW_PROJECT_IF_AVAILABLE, result);
				IProgressMonitor monitor = new NullProgressMonitor();
				try {
					importproject.run(monitor);
					IFile input = (!Util.isEmpty(openInEditors) && result.size() > 0) ? result.get(0).getFile(openInEditors) : null;
					if (input != null && input.exists()) {
						IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), input, true);
					}
					success = true;
				} catch (Exception e) {
					e.printStackTrace();
					StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e), StatusManager.SHOW);
				}
			}	
		} 
		
		if (!success) {
			MessageDialog.openInformation(new Shell(), Messages.ImportExampleProject_ExampleNotFound, MessageFormat.format(Messages.ImportExampleProject_ExampleNotFoundMessage, projectName));
		}

		return success;
	}
	
}
