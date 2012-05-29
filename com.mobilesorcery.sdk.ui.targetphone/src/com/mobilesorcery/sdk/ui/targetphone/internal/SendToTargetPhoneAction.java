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
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResourceChangeEvent;
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
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhoneTransport;
import com.mobilesorcery.sdk.ui.targetphone.TargetPhonePlugin;
import com.mobilesorcery.sdk.ui.targetphone.TargetPhoneTransportEvent;
import com.mobilesorcery.sdk.ui.targetphone.internal.bt.BTTargetPhone;

public class SendToTargetPhoneAction implements IWorkbenchWindowActionDelegate {

	class SendToTargetJob extends Job {
		private final ITargetPhone phone;
		private final MoSyncProject project;

		public SendToTargetJob(MoSyncProject project, ITargetPhone phone) {
			super("");
			this.project = project;
			this.phone = phone;
			setRule(project.getWrappedProject());
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
			IProfile targetProfile = phone == null ? null : phone.getPreferredProfile(project.getProfileManagerType());
			return targetProfile;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			ITargetPhone phone = this.phone;

			int workUnits = phone == null ? 4 : 3;
			monitor.beginTask(createProgressMessage(project, phone), workUnits);

			try {
                assertNotNull(project, "No project selected");
                // Fix for MOSYNC-569
                MoSyncBuilder.saveAllEditors(project.getWrappedProject(), true, true);

				int profileManagerType = project.getProfileManagerType();
				if (phone == null || phone.getPreferredProfile(profileManagerType) == null) {
					SubProgressMonitor subMonitor = new SubProgressMonitor(
							monitor, 1);
					phone = SelectTargetPhoneAction.selectPhone(phone, profileManagerType, window,
							subMonitor);
				}

				if (phone != null) {
					IProfile targetProfile = getProfile(project, phone);
					assertNotNull(targetProfile, "No target profile selected");

					IBuildVariant variant = MoSyncBuilder.createVariant(project, targetProfile);
					ITargetPhoneTransport transport = phone.getTransport();
					TargetPhonePlugin.getDefault().notifyListeners(new TargetPhoneTransportEvent(TargetPhoneTransportEvent.PRE_SEND, phone, transport, project, variant));
					File packageToSend = buildBeforeSend(variant, monitor);
					transport.send(window, project, phone,
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

		private File buildBeforeSend(IBuildVariant variant,
				IProgressMonitor monitor) throws CoreException {
			IBuildResult buildResult = null;
            IBuildSession session = MoSyncBuilder.createDefaultBuildSession(variant);
			buildResult = new MoSyncBuilder().build(project
					.getWrappedProject(), session, variant, null,
					new NullProgressMonitor());
			monitor.worked(1);

			Map<String, List<File>> buildArtifacts = buildResult.getBuildResult();

			List<File> fileToSend = buildArtifacts == null ? null : buildResult.getBuildResult().get(IBuildResult.MAIN);
			if (fileToSend != null && !fileToSend.isEmpty()) {
				return fileToSend.get(0);
			}
			throw new CoreException(new Status(IStatus.ERROR,
					TargetPhonePlugin.PLUGIN_ID,
					"Could not build for device"));
		}
	}

	private static MoSyncProject lastSelected;
	private IAction action;
	private ISelection selection;
	IWorkbenchWindow window;

	@Override
	public void dispose() {
	}

	@Override
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	@Override
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
				@Override
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

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.action = action;
		this.selection = selection;
	}

}
