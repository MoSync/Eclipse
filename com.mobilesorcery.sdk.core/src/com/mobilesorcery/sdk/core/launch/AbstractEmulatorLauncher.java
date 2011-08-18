package com.mobilesorcery.sdk.core.launch;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import com.mobilesorcery.sdk.core.BuildVariant;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildState;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IPackager;
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

	/**
	 * The default implementation just checks whether this launcher is
	 * available.
	 * 
	 * @throws CoreException
	 */
	@Override
	public void assertLaunchable(ILaunchConfiguration launchConfig, String mode)
			throws CoreException {
		if (!isAvailable(launchConfig, mode)) {
			IBuildVariant variant = getVariant(launchConfig, mode);
			throw new CoreException(
					new Status(
							IStatus.ERROR,
							CoreMoSyncPlugin.PLUGIN_ID,
							MessageFormat
									.format("Cannot use {0} run in execution mode \\'{1}\\' on platform {2}.",
											getName(), mode,
											variant.getProfile())));
		}
	}

	protected File getPackageToInstall(ILaunchConfiguration launchConfig,
			String mode) throws CoreException {
		IProject project = EmulatorLaunchConfigurationDelegate
				.getProject(launchConfig);
		MoSyncProject mosyncProject = MoSyncProject.create(project);
		IBuildVariant variant = EmulatorLaunchConfigurationDelegate.getVariant(
				launchConfig, mode);
		IBuildState buildState = mosyncProject.getBuildState(variant);
		IBuildResult buildResult = buildState.getBuildResult();
		File packageToInstall = buildResult == null ? null : buildResult
				.getBuildResult();
		return packageToInstall;
	}

	protected void assertCorrectPackager(ILaunchConfiguration launchConfig,
			String packagerId, String errormsg) throws CoreException {
		if (!isCorrectPackager(launchConfig, packagerId)) {
			throw new CoreException(new Status(IStatus.ERROR,
					CoreMoSyncPlugin.PLUGIN_ID, errormsg));
		}
	}

	protected boolean isCorrectPackager(ILaunchConfiguration launchConfig, String packagerId) {
		IProject project;
		try {
			project = EmulatorLaunchConfigurationDelegate
					.getProject(launchConfig);
			MoSyncProject mosyncProject = MoSyncProject.create(project);
			IProfile targetProfile = mosyncProject.getTargetProfile();
			IPackager packager = targetProfile.getPackager();
			return packagerId.equals(packager.getId());
		} catch (CoreException e) {
			return false;
		}
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * The default behaviour is to make this emulator launcher available in
	 * non-debug modes
	 */
	@Override
	public boolean isAvailable(ILaunchConfiguration launchConfiguration,
			String mode) {
		return !EmulatorLaunchConfigurationDelegate.isDebugMode(mode);
	}

	/**
	 * The default behaviour is to return a non-finalizing build with the build
	 * configuration as per specified by the launch configuration and a target
	 * profile set to the currently selected profile.
	 */
	@Override
	public IBuildVariant getVariant(ILaunchConfiguration launchConfig,
			String mode) throws CoreException {
		IProject project = EmulatorLaunchConfigurationDelegate
				.getProject(launchConfig);
		MoSyncProject mosyncProject = MoSyncProject.create(project);
		IBuildConfiguration cfg = EmulatorLaunchConfigurationDelegate
				.getAutoSwitchBuildConfiguration(launchConfig, mode);
		return new BuildVariant(mosyncProject.getTargetProfile(), cfg, true);
	}

	protected void assertWindows() throws CoreException {
		if (System.getProperty("os.name").toLowerCase().indexOf("win") == -1) {
			throw new CoreException(new Status(IStatus.ERROR,
					CoreMoSyncPlugin.PLUGIN_ID, MessageFormat.format(
							"{0} launches are only supported on Windows",
							getName())));
		}
	}

	protected void assertOSX() throws CoreException {
		if (System.getProperty("os.name").toLowerCase().indexOf("mac") == -1) {
			throw new CoreException(new Status(IStatus.ERROR,
					CoreMoSyncPlugin.PLUGIN_ID, MessageFormat.format(
							"{0} launches are only supported on Mac OS X",
							getName())));
		}
	}
}
