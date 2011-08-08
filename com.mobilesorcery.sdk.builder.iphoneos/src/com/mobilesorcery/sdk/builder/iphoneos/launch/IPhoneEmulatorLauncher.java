package com.mobilesorcery.sdk.builder.iphoneos.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import com.mobilesorcery.sdk.core.launch.AbstractEmulatorLauncher;

public class IPhoneEmulatorLauncher extends AbstractEmulatorLauncher {

	protected IPhoneEmulatorLauncher(String name) {
		super("iPhone Emulator");
	}
	

	@Override
	public void launch(ILaunchConfiguration launchConfig, String mode,
			ILaunch launch, int emulatorId, IProgressMonitor monitor)
			throws CoreException {
		// TODO Auto-generated method stub
		
	}

}
