/*  Copyright (C) 2011 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.mobilesorcery.sdk.ui.internal.handlers;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.ui.MoSyncCommandHandler;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.internal.DefaultProfileFilterDialog;

/**
 * A command handler for filtering the set of profiles associated with
 * a project.
 * @author mattias.bybro@mosync.com
 *
 */
public class FilterProfilesHandler extends MoSyncCommandHandler implements PropertyChangeListener {

	public FilterProfilesHandler() {
		MosyncUIPlugin.getDefault().addListener(this);
	}

	@Override
	public void dispose() {
		MosyncUIPlugin.getDefault().removeListener(this);
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShell(event);
		MoSyncProject project = MosyncUIPlugin.getDefault().getCurrentlySelectedProject(HandlerUtil.getActiveWorkbenchWindow(event));
		if (project != null) {
			Integer projectManagerType =
					PropertyUtil.getInteger(project, MoSyncProject.PROFILE_MANAGER_TYPE_KEY);
			boolean openDialog = projectManagerType != null &&
				projectManagerType == MoSyncTool.DEFAULT_PROFILE_TYPE;

			if (!openDialog) {
				if (askForConversion(shell, project)) {
					PropertyUtil.setInteger(project, MoSyncProject.PROFILE_MANAGER_TYPE_KEY, MoSyncTool.DEFAULT_PROFILE_TYPE);
					openDialog = true;
				}
			}

			if (openDialog) {
				DefaultProfileFilterDialog dialog = new DefaultProfileFilterDialog(shell);
				dialog.setProject(project);
				dialog.open();
			}
		}
		return null;
	}

	private boolean askForConversion(Shell shell, MoSyncProject project) {
		return MessageDialog.openQuestion(shell, "Upgrade profile handling",
				"This project uses device-based profiles. Would you like to convert to platform-based profiles instead?");
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName() == MosyncUIPlugin.CURRENT_PROJECT_CHANGED) {
			MoSyncProject project = MosyncUIPlugin.getDefault().getCurrentlySelectedProject(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
			setBaseEnabled(project != null);
		}
	}

}
