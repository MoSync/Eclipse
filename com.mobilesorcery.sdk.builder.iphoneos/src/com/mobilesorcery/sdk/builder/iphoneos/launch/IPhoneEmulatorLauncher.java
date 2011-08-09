package com.mobilesorcery.sdk.builder.iphoneos.launch;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import com.mobilesorcery.sdk.builder.iphoneos.Activator;
import com.mobilesorcery.sdk.builder.iphoneos.IPhoneOSPackager;
import com.mobilesorcery.sdk.builder.iphoneos.IPhoneSimulator;
import com.mobilesorcery.sdk.core.BuildVariant;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.launch.AbstractEmulatorLauncher;

public class IPhoneEmulatorLauncher extends AbstractEmulatorLauncher {

	public final static String SDK_ATTR = "iphone.sdk";
	
	public IPhoneEmulatorLauncher() {
		super("iPhone Emulator");
	}
	

	@Override
	public void launch(ILaunchConfiguration launchConfig, String mode,
			ILaunch launch, int emulatorId, IProgressMonitor monitor)
			throws CoreException {
		// TODO: Incremental building if we change the SDK!?
		String sdk = launchConfig.getAttribute(SDK_ATTR, "");
		File pathToApp = getPackageToInstall(launchConfig, mode);
		IPhoneSimulator.getDefault().runApp(new Path(pathToApp.getAbsolutePath()), sdk);
	}
	
	@Override
	public IBuildVariant getVariant(ILaunchConfiguration launchConfig, String mode) throws CoreException {
		IBuildVariant prototype = super.getVariant(launchConfig, mode);
		BuildVariant modified = new BuildVariant(prototype);
		modified.setSpecifier(Activator.IOS_SIMULATOR_SPECIFIER, Activator.IOS_SIMULATOR_SPECIFIER);
		return modified;
	}

}
