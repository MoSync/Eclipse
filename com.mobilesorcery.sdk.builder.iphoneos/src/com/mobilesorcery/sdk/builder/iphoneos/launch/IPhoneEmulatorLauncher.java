package com.mobilesorcery.sdk.builder.iphoneos.launch;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import com.mobilesorcery.sdk.builder.iphoneos.Activator;
import com.mobilesorcery.sdk.builder.iphoneos.IPhoneOSPackager;
import com.mobilesorcery.sdk.builder.iphoneos.IPhoneSimulator;
import com.mobilesorcery.sdk.builder.iphoneos.SDK;
import com.mobilesorcery.sdk.builder.iphoneos.XCodeBuild;
import com.mobilesorcery.sdk.core.BuildVariant;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Version;
import com.mobilesorcery.sdk.core.launch.AbstractEmulatorLauncher;
import com.mobilesorcery.sdk.internal.launch.EmulatorLaunchConfigurationDelegate;

public class IPhoneEmulatorLauncher extends AbstractEmulatorLauncher {

	public final static String SDK_ATTR = "iphone.sdk";
	
	public IPhoneEmulatorLauncher() {
		super("iPhone Emulator");
	}

	@Override
	public void assertLaunchable(ILaunchConfiguration launchConfig, String mode) throws CoreException {
		assertOSX();
		IPhoneSimulator.getDefault().assertValid();
		assertCorrectPackager(launchConfig, IPhoneOSPackager.ID, "The iPhone Emulator requires the target profile to be an iOS device");
		super.assertLaunchable(launchConfig, mode);
	}
	
	@Override
	public void launch(ILaunchConfiguration launchConfig, String mode,
			ILaunch launch, int emulatorId, IProgressMonitor monitor)
			throws CoreException {
		// TODO: Incremental building if we change the SDK!?
		IProject project = EmulatorLaunchConfigurationDelegate.getProject(launchConfig);
		MoSyncProject mosyncProject = MoSyncProject.create(project);
		SDK sdk = Activator.getDefault().getSDK(mosyncProject, XCodeBuild.IOS_SIMULATOR_SDKS);
		Version sdkVersion = sdk == null ? null : sdk.getVersion();
		File pathToApp = getPackageToInstall(launchConfig, mode);
		String family = getFamily(getVariant(launchConfig, mode));
		IPhoneSimulator.getDefault().runApp(new Path(pathToApp.getAbsolutePath()), sdkVersion == null ? null : sdkVersion.toString(), family);
	}

	private String getFamily(IBuildVariant variant) {
		// Hard-coded, we may want to get this from device db instead.
		if (variant.getProfile().getName().contains("iPad")) {
			return "ipad";
		}
		return null;
	}
	
	@Override
	public IBuildVariant getVariant(ILaunchConfiguration launchConfig, String mode) throws CoreException {
		IBuildVariant prototype = super.getVariant(launchConfig, mode);
		BuildVariant modified = new BuildVariant(prototype);
		modified.setSpecifier(Activator.IOS_SIMULATOR_SPECIFIER, Activator.IOS_SIMULATOR_SPECIFIER);
		return modified;
	}

}
