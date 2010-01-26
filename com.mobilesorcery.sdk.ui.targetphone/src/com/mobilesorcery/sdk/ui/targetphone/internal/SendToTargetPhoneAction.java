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
package com.mobilesorcery.sdk.ui.targetphone.internal;

import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.Policy;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.targetphone.Activator;

public class SendToTargetPhoneAction implements IWorkbenchWindowActionDelegate {

	private final class SendJob extends Job {
		private final TargetPhone[] selectedPhone;

		private SendJob(String name, TargetPhone[] selectedPhone) {
			super(name);
			this.selectedPhone = selectedPhone;
		}

		protected IStatus run(IProgressMonitor monitor) {
			if (selectedPhone[0] != null) {
				monitor.beginTask("Sending to target", 5);

				if (!selectedPhone[0].isPortAssigned()) {
					IStatus status = SelectTargetPhoneAction.assignPort(window, 
							new SubProgressMonitor(monitor, 1),
							selectedPhone[0], true);
					if (status.getSeverity() != IStatus.OK) {
						return status;
					}
				} else {
					monitor.worked(1);
				}

				MoSyncProject project = MosyncUIPlugin.getDefault()
						.getCurrentlySelectedProject(window);

				if (project == null) {
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
							"No MoSync Project selected");
				}

				IProfile targetProfile = selectedPhone[0].getPreferredProfile();

				if (targetProfile == null) {
					targetProfile = project.getTargetProfile();
				}

				if (targetProfile == null) {
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
							"No Target Profile selected");
				}

				IBuildResult buildResult = null;
				try {
					buildResult = new MoSyncBuilder().fullBuild(project
							.getWrappedProject(), targetProfile, true, true,
							new NullProgressMonitor());
					monitor.worked(1);
				} catch (CoreException e) {
					return new Status(
							IStatus.ERROR,
							Activator.PLUGIN_ID,
							MessageFormat
									.format(
											"Could not build for target {0}. Root cause: {1}",
											targetProfile, e.getMessage()), e);
				}

				if (buildResult != null) {
					try {
						if (buildResult.getBuildResult() == null) {
							return new Status(IStatus.ERROR,
									Activator.PLUGIN_ID,
									"Could not build for device");
						}
						Bcobex.sendObexFile(selectedPhone[0], buildResult
								.getBuildResult(), new SubProgressMonitor(
								monitor, 3));
						return Status.OK_STATUS;
					} catch (IOException e) {
						return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
								"Could not send file to device", e);
					}
				}
			}
			return Status.OK_STATUS;
		}
	}

	private static MoSyncProject lastSelected;
	private IAction action;
	private ISelection selection;
	private IWorkbenchWindow window;

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public void run(IAction action) {
		final TargetPhone phone = Activator.getDefault()
				.getCurrentlySelectedPhone();

		final TargetPhone[] selectedPhone = new TargetPhone[1];
		selectedPhone[0] = phone;

		if (phone == null) {
			try {
				selectedPhone[0] = SelectTargetPhoneAction.selectPhone();
			} catch (IOException e) {
				Policy.getStatusHandler().show(
						new Status(IStatus.ERROR, Activator.PLUGIN_ID, e
								.getMessage(), e), e.toString());
			}

			if (selectedPhone[0] == null) {
				return; // Cancelled
			}
		}

		Job sendJob = new SendJob("Send to target", selectedPhone);

		sendJob.setUser(true);
		sendJob.schedule();
	}

	/**
	 * Sends the build result to the target phone
	 */
	public void sendTo(final TargetPhone phone) {
		if (phone != null) {
			Job job = new Job("Send to target") {
				public IStatus run(IProgressMonitor monitor) {
					IPath mobex = MoSyncTool.getDefault().getMoSyncBin()
							.append("mobex.exe");
					MoSyncProject project = MosyncUIPlugin.getDefault()
							.getCurrentlySelectedProject(window);

					// project.get
					IProfile currentProfile = project.getTargetProfile();

					int result = 0;

					if (project != null) {
						String[] commandLine = getCommandLine(mobex, "", phone);
						try {
							Process process = Runtime.getRuntime().exec(
									commandLine);
							result = process.waitFor();
						} catch (Exception e) {
							result = -1;
						}
					}

					return result == 0 ? Status.OK_STATUS : new Status(
							Status.ERROR, Activator.PLUGIN_ID,
							"Could not send to phone");
				}

			};
		}
	}

	private String[] getCommandLine(IPath mobex, String fileToSend,
			TargetPhone phone) {
		return new String[] { mobex.toFile().getAbsolutePath(), fileToSend,
				phone.getAddress(), Integer.toString(phone.getPort()) };
	}

	public void selectionChanged(IAction action, ISelection selection) {
		this.action = action;
		this.selection = selection;
	}

}
