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
package com.mobilesorcery.sdk.ui.targetphone.internal.bt;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.window.IShellProvider;

import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhone;
import com.mobilesorcery.sdk.ui.targetphone.TargetPhonePlugin;
import com.mobilesorcery.sdk.ui.targetphone.TargetPhoneTransportEvent;

final class BTSendJob extends Job {

	private final BTTargetPhone selectedPhone;
	private IShellProvider shellProvider;
	private File packageToSend;
	private MoSyncProject project;
	private IBuildVariant variant;

	BTSendJob(IShellProvider shellProvider, BTTargetPhone selectedPhone,
			File packageToSend, MoSyncProject project, IBuildVariant variant) {
		super("Sending to device");
		this.shellProvider = shellProvider;
		this.selectedPhone = selectedPhone;
		this.packageToSend = packageToSend;
		this.project = project;
		this.variant = variant;
	}

	protected IStatus run(IProgressMonitor monitor) {
		try {
			if (selectedPhone != null) {
				monitor.beginTask("Sending to target", 2);

				IStatus status = beforeSend(new SubProgressMonitor(monitor, 1));
				if (status.getSeverity() != IStatus.OK) {
					return status;
				}

				try {
					sendBuildResult(packageToSend, new SubProgressMonitor(
							monitor, 1));
				} catch (CoreException e) {
					return e.getStatus();
				}
			}
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}

	protected ITargetPhone getSelectedPhone() {
		return selectedPhone;
	}

	protected IStatus beforeSend(IProgressMonitor monitor) {
		IStatus status = Status.OK_STATUS;
		if (!selectedPhone.isPortAssigned()) {
			status = BTTargetPhoneTransport.assignPort(shellProvider,
					new SubProgressMonitor(monitor, 1), selectedPhone);
		}

		monitor.done();
		return status;
	}

	protected void sendBuildResult(File buildResult, IProgressMonitor monitor)
			throws CoreException {
		try {
			TargetPhonePlugin.getDefault().notifyListeners(new TargetPhoneTransportEvent(TargetPhoneTransportEvent.ABOUT_TO_LAUNCH, selectedPhone, project, variant));
			Bcobex.sendObexFile(selectedPhone, buildResult, monitor);
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR,
					TargetPhonePlugin.PLUGIN_ID, "Unable to send to device", e));
		}
	}

	public void runSync(IProgressMonitor monitor) throws CoreException {
		IStatus status = run(monitor);
		if (status.getSeverity() == IStatus.ERROR) {
			throw new CoreException(status);
		}
	}
}