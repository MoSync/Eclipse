package com.mobilesorcery.sdk.internal.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.launch.IEmulatorLauncher;

public class EmulatorLauncherProxy implements IEmulatorLauncher {

	private IConfigurationElement element;
	private IEmulatorLauncher delegate;
	
	public EmulatorLauncherProxy(IConfigurationElement element) {
		this.element = element;
	}
	
	@Override
	public void launch(ILaunchConfiguration launchConfig, String mode,
			ILaunch launch, int emulatorId, IProgressMonitor monitor)
			throws CoreException {
		initDelegate();
		delegate.launch(launchConfig, mode, launch, emulatorId, monitor);
	}

	private void initDelegate() {
		try {
			if (element != null) {
				delegate = (IEmulatorLauncher) element.createExecutableExtension("implementation");
			}
		} catch (Exception e) {
			CoreMoSyncPlugin.getDefault().log(e);
		} finally {
			element = null;
		}
		
	}

	@Override
	public String getName() {
		initDelegate();
		return delegate.getName();
	}

	@Override
	public boolean isAvailable(MoSyncProject project, String mode) {
		initDelegate();
		return delegate.isAvailable(project, mode);
	}

	@Override
	public IBuildVariant getVariant(ILaunchConfiguration launchConfig,
			String mode) throws CoreException {
		initDelegate();
		return delegate.getVariant(launchConfig, mode);
	}

}
