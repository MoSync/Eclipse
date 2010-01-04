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
package com.mobilesorcery.sdk.update.internal;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.update.UpdateManager;

public class ResendConfirmationCodeAction extends Action {

	private IWorkbenchWindow window;
	
	public ResendConfirmationCodeAction() {
		// Hack.
		window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	}

	private Shell getShell() {
		return window.getShell();
	}
	
	public void run() {
		Job resendJob = new Job(Messages.ResendConfirmationCodeAction_JobTitle) {
			protected IStatus run(IProgressMonitor monitor) {
				boolean result;
				try {
					result = UpdateManager.getDefault().resend();
					if (result) {
						UpdateProfilesAction.showInfo(getShell(),
								Messages.ResendConfirmationCodeAction_ConfirmationEmailMessage);
					} else {
						UpdateProfilesAction.showInfo(getShell(), Messages.ResendConfirmationCodeAction_ConfirmationEmailError);
					}
					
					return Status.OK_STATUS;
				} catch (Exception e) {
					return new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, e.getMessage(), e);
				}
			}
		};
		
		resendJob.setUser(true);
		resendJob.schedule();
	}

}
