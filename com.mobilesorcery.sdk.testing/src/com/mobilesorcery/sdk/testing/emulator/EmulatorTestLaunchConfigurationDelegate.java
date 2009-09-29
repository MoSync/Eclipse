package com.mobilesorcery.sdk.testing.emulator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.internal.launch.EmulatorLaunchConfigurationDelegate;
import com.mobilesorcery.sdk.testing.ITestSession;
import com.mobilesorcery.sdk.testing.TestManager;
import com.mobilesorcery.sdk.testing.TestSession;

public class EmulatorTestLaunchConfigurationDelegate extends
		EmulatorLaunchConfigurationDelegate {

	// TODO: There should be just a switch or something to indicate what entry point to run - if any!?
	public void launchSync(ILaunchConfiguration launchConfig, String mode, ILaunch launch, int emulatorId, IProgressMonitor monitor) throws CoreException {
		EmulatorTestRunner suite = new EmulatorTestRunner(launchConfig, launchConfig.getName(), emulatorId);
		ITestSession session = suite.createTestSession();
		TestManager.getInstance().addTestSession(session);
		try {
			session.start();
			super.launchSync(launchConfig, mode, launch, emulatorId, monitor);
		} finally {
			session.finish();
		}
	}
}
