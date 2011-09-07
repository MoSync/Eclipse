package com.mobilesorcery.sdk.internal.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.launch.IEmulatorLauncher;

public class EmulatorLauncherProxy implements IEmulatorLauncher {

	private IConfigurationElement element;
	private IEmulatorLauncher delegate;
	private final String id;

	public EmulatorLauncherProxy(IConfigurationElement element) {
		this.element = element;
		this.id = element.getAttribute("id");
	}

	@Override
	public final String getId() {
		return id;
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
	public int isLaunchable(ILaunchConfiguration launchConfig, String mode) {
		initDelegate();
		return delegate.isLaunchable(launchConfig, mode);
	}

	@Override
	public IBuildVariant getVariant(ILaunchConfiguration launchConfig,
			String mode) throws CoreException {
		initDelegate();
		return delegate.getVariant(launchConfig, mode);
	}

	@Override
	public void setDefaultAttributes(ILaunchConfigurationWorkingCopy wc) {
		initDelegate();
		delegate.setDefaultAttributes(wc);
	}

	@Override
	public String configure(ILaunchConfiguration config, String mode) {
		initDelegate();
		return delegate.configure(config, mode);
	}
}
