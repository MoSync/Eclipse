package com.mobilesorcery.sdk.core.launch;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import com.mobilesorcery.sdk.core.BuildVariant;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildState;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.ILaunchConstants;
import com.mobilesorcery.sdk.core.IPackager;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.internal.launch.EmulatorLaunchConfigurationDelegate;
import com.mobilesorcery.sdk.profiles.IProfile;

public abstract class AbstractEmulatorLauncher implements IEmulatorLauncher {

	private final String name;

	protected AbstractEmulatorLauncher(String name) {
		this.name = name;
	}

	@Override
	public String getId() {
		throw new UnsupportedOperationException();
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
		List<File> buildArtifacts = buildResult == null ? null : buildResult
				.getBuildResult().get(IBuildResult.MAIN);
		File packageToInstall = buildArtifacts == null || buildArtifacts.isEmpty() ? null : buildArtifacts.get(0);
		return packageToInstall;
	}

	protected void assertCorrectPackager(ILaunchConfiguration launchConfig, String errormsg) throws CoreException {
		if (!isCorrectPackager(launchConfig)) {
			throw new CoreException(new Status(IStatus.ERROR,
					CoreMoSyncPlugin.PLUGIN_ID, errormsg));
		}
	}

	protected boolean isCorrectPackager(ILaunchConfiguration launchConfig) {
		IProject project;
		try {
			project = EmulatorLaunchConfigurationDelegate
					.getProject(launchConfig);
			MoSyncProject mosyncProject = MoSyncProject.create(project);
			IProfile targetProfile = mosyncProject.getTargetProfile();
			IPackager packager = targetProfile.getPackager();
			int launchType = getLaunchType(packager);
			return launchType != LAUNCH_TYPE_NONE;
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
	 * all modes that {@link #supportsLaunchMode(String)} returns {@code true} for.
	 */
	@Override
	public int isLaunchable(ILaunchConfiguration launchConfiguration, String mode) {
		return supportsLaunchMode(mode) ? LAUNCHABLE : UNLAUNCHABLE;
	}
	
	public boolean supportsLaunchMode(String mode) {
		return true;
	}

	/**
	 * The default behaviour is to return a non-finalizing build with the build
	 * configuration as per specified by the launch configuration and a target
	 * profile set to the currently selected profile.
	 */
	@Override
	public IBuildVariant getVariant(ILaunchConfiguration launchConfig, String mode) throws CoreException {
		return getVariantDefault(launchConfig, mode);
	}

	/**
	 * Returns a build with the build
	 * configuration as per specified by the launch configuration and a target
	 * profile set to the currently selected profile.
	 * @param launchConfig
	 * @param mode
	 * @return
	 * @throws CoreException
	 */
	protected final IBuildVariant getVariantDefault(ILaunchConfiguration launchConfig, String mode) throws CoreException {
		IProject project = EmulatorLaunchConfigurationDelegate
				.getProject(launchConfig);
		MoSyncProject mosyncProject = MoSyncProject.create(project);
		IBuildConfiguration cfg = EmulatorLaunchConfigurationDelegate
				.getAutoSwitchBuildConfiguration(launchConfig, mode);
		return new BuildVariant(mosyncProject.getTargetProfile(), cfg);
	}
	
	protected boolean isOnDevice(ILaunchConfiguration config) {
		try {
			return config.getAttribute(ILaunchConstants.ON_DEVICE, false);
		} catch (CoreException e) {
			return false;
		}
	}

	@Override
	public void setDefaultAttributes(ILaunchConfigurationWorkingCopy wc) {
		// Default impl does nothing.
	}

	@Override
	public IEmulatorLauncher configure(ILaunchConfiguration config, String mode) {
		return this;
	}

	/**
	 * Returns whether the launch configuration is an automatic selection launch
	 * @param config
	 * @param mode
	 * @return
	 */
	protected boolean isAutoSelectLaunch(ILaunchConfiguration config, String mode) {
		try {
			IEmulatorLauncher launcher = EmulatorLaunchConfigurationDelegate.getEmulatorLauncher(config, mode);
			return AutomaticEmulatorLauncher.ID.equals(launcher.getId());
		} catch (CoreException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * For automatic selection launches; checks whether the user should be asked about which launcher to use.
	 * @param packagerId
	 * @return
	 */
	protected boolean shouldAskUserForLauncher(String packagerId) {
		IEmulatorLauncher preferredLauncher = CoreMoSyncPlugin.getDefault().getPreferredLauncher(packagerId);
		return preferredLauncher == null;
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
