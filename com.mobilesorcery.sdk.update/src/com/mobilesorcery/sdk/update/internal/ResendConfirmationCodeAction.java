package com.mobilesorcery.sdk.update.internal;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
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

	public void run() {
		Job resendJob = new Job(Messages.ResendConfirmationCodeAction_JobTitle) {
			protected IStatus run(IProgressMonitor monitor) {
				boolean result;
				try {
					result = UpdateManager.getDefault().resend();
					if (result) {
						UpdateProfilesAction.showInfo(window,
								Messages.ResendConfirmationCodeAction_ConfirmationEmailMessage);
					} else {
						UpdateProfilesAction.showInfo(window, Messages.ResendConfirmationCodeAction_ConfirmationEmailError);
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
