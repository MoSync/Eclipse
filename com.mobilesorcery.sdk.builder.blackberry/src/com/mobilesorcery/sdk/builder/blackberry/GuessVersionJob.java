package com.mobilesorcery.sdk.builder.blackberry;

import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.mobilesorcery.sdk.core.Version;

public class GuessVersionJob extends Job {

	private JDE jde;
	private Version version;

	public GuessVersionJob() {
		super("Computing JDE version...");
	}

	public void setJDE(JDE jde) {
		this.jde = jde;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			version = jde.guessVersion();
			return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
		} catch (IOException e) {
			return new Status(IStatus.ERROR, BlackBerryPlugin.PLUGIN_ID, "Could not guess JDE verson", e);
		}
	}

	public Version getVersion() {
		return version;
	}

}
