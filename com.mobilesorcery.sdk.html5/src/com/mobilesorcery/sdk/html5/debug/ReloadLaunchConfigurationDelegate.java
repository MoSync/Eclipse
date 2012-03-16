package com.mobilesorcery.sdk.html5.debug;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;
import org.eclipse.wst.jsdt.debug.internal.core.launching.JavaScriptProcess;
import org.eclipse.wst.jsdt.debug.internal.core.launching.RemoteJavaScriptLaunchDelegate;
import org.eclipse.wst.jsdt.debug.internal.core.model.JavaScriptDebugTarget;

import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadProcess;

public class ReloadLaunchConfigurationDelegate extends RemoteJavaScriptLaunchDelegate implements
		ILaunchConfigurationDelegate {

	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		Html5Plugin.getDefault().startReloadServer();
		String name = "Reload";
		ReloadProcess process = new ReloadProcess(launch, name);
		launch.addProcess(process);
		JSODDDebugTarget target = new JSODDDebugTarget(launch, process);
		launch.addDebugTarget(target);
		super.launch(configuration, mode, launch, monitor);
	}

}
