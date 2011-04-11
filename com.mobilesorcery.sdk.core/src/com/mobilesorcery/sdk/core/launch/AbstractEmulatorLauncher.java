package com.mobilesorcery.sdk.core.launch;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildState;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IPackager;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.internal.launch.EmulatorLaunchConfigurationDelegate;
import com.mobilesorcery.sdk.profiles.IProfile;

public abstract class AbstractEmulatorLauncher implements IEmulatorLauncher {

	private String name;

	protected AbstractEmulatorLauncher(String name) {
		this.name = name;
	}
	
	@Override
	public void launch(ILaunchConfiguration launchConfig, String mode,
			ILaunch launch, int emulatorId, IProgressMonitor monitor)
			throws CoreException {
		// TODO Auto-generated method stub
		
	}
	
	protected File getPackageToInstall(ILaunchConfiguration launchConfig) throws CoreException {
		IProject project = EmulatorLaunchConfigurationDelegate.getProject(launchConfig);
		MoSyncProject mosyncProject = MoSyncProject.create(project);
		IProfile targetProfile = mosyncProject.getTargetProfile();
		IBuildVariant variant = MoSyncBuilder.getFinalizerVariant(mosyncProject, targetProfile);
        IBuildState buildState = mosyncProject.getBuildState(variant);
        IBuildResult buildResult = buildState.getBuildResult();
    	File packageToInstall = buildResult == null ? null : buildResult.getBuildResult();
        return packageToInstall;
	}
	
	protected void assertCorrectPackager(ILaunchConfiguration launchConfig, String id, String errormsg) throws CoreException {
		IProject project = EmulatorLaunchConfigurationDelegate.getProject(launchConfig);
		MoSyncProject mosyncProject = MoSyncProject.create(project);
		IProfile targetProfile = mosyncProject.getTargetProfile();
		IPackager packager = targetProfile.getPackager();
		if (!id.equals(packager.getId())) {
			throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, errormsg));
		}
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * The default behaviour is to make this emulator launcher available in non-debug modes
	 */
	@Override
	public boolean isAvailable(MoSyncProject project, String mode) {
		return !EmulatorLaunchConfigurationDelegate.isDebugMode(mode);
	}

}
