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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;

import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.importproject.Activator;
import com.mobilesorcery.sdk.importproject.ImportProjectsRunnable;


public class ImportExampleProject {

	private String projectName;
	private String projectExtension;
	private String projectExtraDir;
	public ImportExampleProject(String project, String extension, String extradir) {
		projectName = project;
		projectExtension = extension;
		projectExtraDir = extradir;
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
				ImportProjectsRunnable importproject = new ImportProjectsRunnable(fileChildren, ImportProjectsRunnable.COPY_ALL_FILES);
				IProgressMonitor monitor = new NullProgressMonitor();
				try {
					importproject.run(monitor);
					success = true;
				} catch (InvocationTargetException e) {
					e.printStackTrace();
					StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e), StatusManager.SHOW);
				} catch (InterruptedException e) {
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
