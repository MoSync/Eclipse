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
package com.mobilesorcery.sdk.ui.internal.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncProject;

public class ManageBuildConfigsAction extends Action implements IObjectActionDelegate {

	private static final String BUILD_CONFIG_SETTINGS_ID = "com.mobilesorcery.sdk.ui.properties.buildconfigs";
	private static final String BUILD_SETTINGS_ID = "com.mobilesorcery.sdk.ui.properties.buildsettings";
	
	private MoSyncProject project;
	private IWorkbenchPart targetPart;

	public ManageBuildConfigsAction() {
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
	}

	public void run() {
		run(this);
	}
	
	public void run(IAction action) {
		PreferenceDialog dialog = PreferencesUtil.createPropertyDialogOn(
				targetPart.getSite().getWorkbenchWindow().getShell(), 
				project.getWrappedProject(), BUILD_CONFIG_SETTINGS_ID,
				new String[] { BUILD_CONFIG_SETTINGS_ID, BUILD_SETTINGS_ID },
				null, PreferencesUtil.OPTION_FILTER_LOCKED);
		
		dialog.open();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		IStructuredSelection ssel = (IStructuredSelection) selection;
		project = CoreMoSyncPlugin.getDefault().extractProject(ssel.toList());		
	}

}
