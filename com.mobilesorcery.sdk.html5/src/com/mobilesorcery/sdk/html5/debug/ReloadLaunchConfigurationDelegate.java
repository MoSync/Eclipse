package com.mobilesorcery.sdk.html5.debug;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadProcess;
import com.mobilesorcery.sdk.internal.launch.EmulatorLaunchConfigurationDelegate;

public class ReloadLaunchConfigurationDelegate extends EmulatorLaunchConfigurationDelegate {

	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		String name = "Reload";
		ReloadProcess process = new ReloadProcess(launch, name);
		process.start();
		launch.addProcess(process);
		JSODDDebugTarget target = new JSODDDebugTarget(launch, process);
		launch.addDebugTarget(target);
		super.launch(configuration, mode, launch, monitor);
	}
	

}
