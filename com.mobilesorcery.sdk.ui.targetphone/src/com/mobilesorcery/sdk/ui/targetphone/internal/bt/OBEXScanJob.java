/**
 * 
 */
package com.mobilesorcery.sdk.ui.targetphone.internal.bt;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.window.IShellProvider;

public final class OBEXScanJob extends Job {
	private final BTTargetPhone phone;
	private IShellProvider shellProvider;

	public OBEXScanJob(IShellProvider shellProvider, BTTargetPhone phone) {
		super("Scanning for OBEX service");
		this.shellProvider = shellProvider;
		this.phone = phone;
	}

	protected IStatus run(IProgressMonitor monitor) {
		return BTTargetPhoneTransport.assignPort(shellProvider, monitor, phone);
	}
}