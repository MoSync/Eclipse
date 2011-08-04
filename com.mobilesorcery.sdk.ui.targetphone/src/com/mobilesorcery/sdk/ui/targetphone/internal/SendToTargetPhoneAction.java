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

import java.io.File;
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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhone;
import com.mobilesorcery.sdk.ui.targetphone.TargetPhonePlugin;
import com.mobilesorcery.sdk.ui.targetphone.internal.bt.BTTargetPhone;

public class SendToTargetPhoneAction implements IWorkbenchWindowActionDelegate {

	class SendToTargetJob extends Job {
		private ITargetPhone phone;
		private MoSyncProject project;

		public SendToTargetJob(MoSyncProject project, ITargetPhone phone) {
			super("");
			this.project = project;
			this.phone = phone;
			setName(createProgressMessage(project, phone));
		}

		private String createProgressMessage(MoSyncProject project,
				ITargetPhone phone) {
		    if (project == null || phone == null) {
		        return "Send to target";
		    } else {
		        return MessageFormat.format("Send {0} to target {1} for profile {2}", project == null ? "?" : project.getName(), phone == null ? "?" : phone.getName(),
		               MoSyncTool.toString(getProfile(project, phone)));
		    }
		}

		private IProfile getProfile(MoSyncProject project, ITargetPhone phone) {
			IProfile targetProfile = phone == null ? null : phone.getPreferredProfile();
			if (targetProfile == null) {
				targetProfile = project == null ? null : project.getTargetProfile();
			}
			return targetProfile;
		}

		protected IStatus run(IProgressMonitor monitor) {
			ITargetPhone phone = this.phone;

			int workUnits = phone == null ? 4 : 3;
			monitor.beginTask(createProgressMessage(project, phone), workUnits);

			try {
                assertNotNull(project, "No project selected");
				if (phone == null) {
					SubProgressMonitor subMonitor = new SubProgressMonitor(
							monitor, 1);
					phone = SelectTargetPhoneAction.selectPhone(window,
							subMonitor);
				}

				if (phone != null) {
					IProfile targetProfile = getProfile(project, phone);
					assertNotNull(targetProfile, "No target profile selected");
					
					File packageToSend = buildBeforeSend(targetProfile, monitor);
					phone.getTransport().send(window, project, phone,
							packageToSend, new SubProgressMonitor(monitor, 4));
				}
			} catch (CoreException e) {
				return e.getStatus();
			}
			return Status.OK_STATUS;
		}

		private void assertNotNull(Object o, String msgIfNull) throws CoreException {
			if (o == null) {
				throw new CoreException(new Status(IStatus.ERROR, TargetPhonePlugin.PLUGIN_ID, msgIfNull));
			}
		}

		private File buildBeforeSend(IProfile targetProfile,
				IProgressMonitor monitor) throws CoreException {
			IBuildResult buildResult = null;
            IBuildVariant variant = MoSyncBuilder.getFinalizerVariant(project, targetProfile);
            IBuildSession session = MoSyncBuilder.createDefaultBuildSession(variant);
			buildResult = new MoSyncBuilder().build(project
					.getWrappedProject(), session, variant, null,
					new NullProgressMonitor());
			monitor.worked(1);

			if (buildResult.getBuildResult() == null) {
				throw new CoreException(new Status(IStatus.ERROR,
						TargetPhonePlugin.PLUGIN_ID,
						"Could not build for device"));
			}
			
			return buildResult.getBuildResult();
		}
	}

	private static MoSyncProject lastSelected;
	private IAction action;
	private ISelection selection;
	IWorkbenchWindow window;

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public void run(IAction action) {
		ITargetPhone phone = TargetPhonePlugin.getDefault()
				.getCurrentlySelectedPhone();

		MoSyncProject project = MosyncUIPlugin.getDefault()
				.getCurrentlySelectedProject(
						PlatformUI.getWorkbench().getActiveWorkbenchWindow());

		SendToTargetJob job = new SendToTargetJob(project, phone);
		job.setUser(true);
		job.schedule();
	}

	/**
	 * Sends the build result to the target phone CURRENTLY NOT USED
	 */
	public void sendTo(final BTTargetPhone phone) {
		if (phone != null) {
			Job job = new Job("Send to target") {
				public IStatus run(IProgressMonitor monitor) {
					IPath mobex = MoSyncTool.getDefault().getBinary("mobex");
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
							Status.ERROR, TargetPhonePlugin.PLUGIN_ID,
							"Could not send to phone");
				}

			};
		}
	}

	private String[] getCommandLine(IPath mobex, String fileToSend,
			BTTargetPhone phone) {
		return new String[] { mobex.toFile().getAbsolutePath(), fileToSend,
				phone.getAddress(), Integer.toString(phone.getPort()) };
	}

	public void selectionChanged(IAction action, ISelection selection) {
		this.action = action;
		this.selection = selection;
	}

}
