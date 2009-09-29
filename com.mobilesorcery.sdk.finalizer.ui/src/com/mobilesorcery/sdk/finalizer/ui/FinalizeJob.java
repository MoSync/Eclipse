/**
 * 
 */
package com.mobilesorcery.sdk.finalizer.ui;

import java.io.StringReader;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.finalizer.core.FinalizerParser;

public final class FinalizeJob extends Job {

	private String finalizerScript;
	private MoSyncProject project;

	public FinalizeJob(MoSyncProject project, String finalizerScript) {
		super(Messages.FinalizeJob_Finalize);
		this.project = project;
		this.finalizerScript = finalizerScript;
	}

	protected IStatus run(final IProgressMonitor monitor) {
		if (project == null) {
			return new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID,
					Messages.FinalizeJob_NoProjectSelected);
		}

		StringReader script = new StringReader(finalizerScript);
		final FinalizerParser parser = new FinalizerParser(project
				.getWrappedProject());
		try {
			parser.execute(script, monitor);
			return Status.OK_STATUS;
		} catch (Exception e) {
			return new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, e
					.getMessage(), e);
		}
	}
}