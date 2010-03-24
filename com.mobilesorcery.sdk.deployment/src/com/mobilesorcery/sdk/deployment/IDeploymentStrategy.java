package com.mobilesorcery.sdk.deployment;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.profiles.IProfile;

public interface IDeploymentStrategy {

	public String getFactoryId();

	/**
	 * Deploys a number of packages
	 * @param project
	 * @param packages
	 * @param monitor
	 * @throws Exception
	 */
	public void deploy(MoSyncProject project, List<IProfile> profiles, IProgressMonitor monitor) throws Exception;
	
}
