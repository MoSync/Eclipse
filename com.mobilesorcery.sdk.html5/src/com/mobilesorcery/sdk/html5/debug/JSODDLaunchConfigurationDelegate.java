package com.mobilesorcery.sdk.html5.debug;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate2;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;
import org.eclipse.wst.jsdt.debug.internal.core.launching.JavaScriptProcess;
import org.eclipse.wst.jsdt.debug.internal.core.model.JavaScriptDebugTarget;

import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadListeningConnector;

public class JSODDLaunchConfigurationDelegate implements
		ILaunchConfigurationDelegate2 {

	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		try {
			runJSDTRemoteConnector(launch);
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Html5Plugin.PLUGIN_ID, e.getMessage(), e));
		}
	}

	@Override
	public ILaunch getLaunch(ILaunchConfiguration configuration, String mode)
			throws CoreException {
		return null;
	}

	public void runJSDTRemoteConnector(ILaunch launch) throws IOException {
		// This is more or less extracted from JSDTs remote java script launcher
		ReloadListeningConnector connector = ReloadListeningConnector
				.getDefault();
		Map arguments = new HashMap<String, String>();
		VirtualMachine vm = connector.accept(arguments);

		// TODO: refactor
		JavaScriptProcess process = new JavaScriptProcess(launch, "Reload") {
			private IStreamsProxy streams = null;

			public IStreamsProxy getStreamsProxy() {
				if (streams == null) {
					streams = new JSODDStreamsProxy();
				}
				
				return streams;
			}
		};
		launch.addProcess(process);
		
		JavaScriptDebugTarget target = new JavaScriptDebugTarget(vm, process, launch, true, true);
		launch.addDebugTarget(target);
	}

	@Override
	public boolean buildForLaunch(ILaunchConfiguration configuration,
			String mode, IProgressMonitor monitor) throws CoreException {
		// We never really build; this is just for launching the debugger
		return false;
	}

	@Override
	public boolean finalLaunchCheck(ILaunchConfiguration configuration,
			String mode, IProgressMonitor monitor) throws CoreException {
		return true;
	}

	@Override
	public boolean preLaunchCheck(ILaunchConfiguration configuration,
			String mode, IProgressMonitor monitor) throws CoreException {
		// TODO Only JS ODD enabled projects here
		return true;
	}

}
