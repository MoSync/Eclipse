package com.mobilesorcery.sdk.ui.targetphone.internal.bt;

import java.io.IOException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.Policy;
import org.eclipse.ui.IWorkbenchWindow;

import com.mobilesorcery.sdk.ui.targetphone.ITargetPhone;
import com.mobilesorcery.sdk.ui.targetphone.TargetPhonePlugin;

public class BTSelectTargetPhoneAction extends Action {

	private IWorkbenchWindow window;

	public BTSelectTargetPhoneAction() {
		
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}
	
	public void run() {
		try {
			ITargetPhone phone = BTTargetPhoneTransport.selectPhone();
			if (phone == null) {
				return; // Cancelled.
			}
			
			Job job = new OBEXScanJob(window, (BTTargetPhone) phone);

			job.setUser(true);
			job.schedule();
		} catch (IOException e) {
			Policy.getStatusHandler().show(
					new Status(IStatus.ERROR, TargetPhonePlugin.PLUGIN_ID, e
							.getMessage(), e), e.toString());
		}
	}

}
