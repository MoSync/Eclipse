package com.mobilesorcery.sdk.deployment.internal.ui.ftp;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.operation.IRunnableWithProgress;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.deployment.IDeploymentStrategy;
import com.mobilesorcery.sdk.deployment.ProjectDeploymentStrategy;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;

public class DeploymentRunnable implements IRunnableWithProgress {

	private final IDeploymentStrategy strategy;
	private final MoSyncProject project;
	private final File deployFile;
	private final IDeviceFilter profiles;

	public DeploymentRunnable(MoSyncProject project, IDeploymentStrategy strategy, IDeviceFilter profiles, File deployFile) {
		this.project = project;
		this.strategy = strategy;
		this.profiles = profiles;
		this.deployFile = deployFile;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		if (deployFile != null) {
			ProjectDeploymentStrategy pds = new ProjectDeploymentStrategy(project, deployFile);
			pds.addStrategy(strategy);
			pds.assignProfiles(strategy, profiles);
			try {
				pds.saveToMetaFile();
			} catch (Exception e) {
				throw new InvocationTargetException(e, "Could not save deploy file");
			}
		}

		try {
			strategy.deploy(project, Arrays.asList(MoSyncTool.getDefault().getProfileManager(MoSyncTool.DEFAULT_PROFILE_TYPE).getProfiles(profiles)), monitor);
		} catch (OperationCanceledException e) {
		    // Ignore.
		} catch (Exception e) {
			throw new InvocationTargetException(e, e.getMessage());
		}
	}

}
