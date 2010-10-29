package com.mobilesorcery.sdk.capabilities.ui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import com.mobilesorcery.sdk.capabilities.core.ICapabilities;
import com.mobilesorcery.sdk.capabilities.core.ICapabilitiesAnalyzer;
import com.mobilesorcery.sdk.capabilities.core.ICapabilitiesMatcher;
import com.mobilesorcery.sdk.capabilities.core.IChangeRequest;
import com.mobilesorcery.sdk.core.MoSyncProject;

public class CapabilitiesJob extends Job {

	private ICapabilitiesAnalyzer analyzer;
	private IProject[] projects;
	private Shell cachedShell;
	private ICapabilitiesMatcher matcher;

	CapabilitiesJob(Shell shell, ICapabilitiesAnalyzer analyzer, ICapabilitiesMatcher matcher, IProject[] projects) {
		super("Capabilities analysis");
		this.analyzer = analyzer;
		this.matcher = matcher;
		this.projects = projects;
		this.cachedShell = shell;
		Assert.isLegal(cachedShell != null, "Must provide a shell");
	}

	protected IStatus run(IProgressMonitor monitor) {
		try {
			monitor.beginTask(getName(), projects.length);
			final HashMap<IProject, IChangeRequest> changeRequestMap = new HashMap<IProject, IChangeRequest>();
			for (int i = 0; i < projects.length; i++) {
				long now = System.currentTimeMillis();
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1);
				subMonitor.beginTask(projects[i].getName(), 100);
				if (!monitor.isCanceled()) {
					// TODO: extension points also for capabilities analyzers!
					ICapabilities requestedCapabilites = analyzer.analyze(projects[i], subMonitor);
					IChangeRequest changeRequest = matcher.match(MoSyncProject.create(projects[i]), requestedCapabilites);
					if (changeRequest != null) {
						changeRequestMap.put(projects[i], changeRequest);
					}
					subMonitor.done();
				}
			}
			
			if (!monitor.isCanceled()) {
				// TODO: Consider doing the job in the wizard!?
				cachedShell.getDisplay().asyncExec(new Runnable() {
					public void run() {
						if (changeRequestMap.isEmpty()) {
							showNoChangesRequiredDialog();
						} else {
							showChangeRequestWizard(changeRequestMap);
						}
					}
	
				});
			}

			// TODO: Job.ASYNC_FINISH?
			return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
		} catch (CoreException e) {
			return e.getStatus();
		}
	}


	void showNoChangesRequiredDialog() {
		MessageDialog.openInformation(cachedShell, "Capability analysis done", "Capabilites analysis:\nNo changes to the project necessary");
	}				
	
	void showChangeRequestWizard(Map<IProject, IChangeRequest> changeRequests) {
		CapabilitiesAnalyzerWizard capabilitesAnalysisWizard = new CapabilitiesAnalyzerWizard();
		capabilitesAnalysisWizard.setChangeRequests(changeRequests);
		WizardDialog wizardDialog = new WizardDialog(cachedShell, capabilitesAnalysisWizard);
		wizardDialog.open();
	}
}
