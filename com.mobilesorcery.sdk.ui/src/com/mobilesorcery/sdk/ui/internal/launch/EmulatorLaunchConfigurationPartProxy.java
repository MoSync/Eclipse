package com.mobilesorcery.sdk.ui.internal.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.widgets.Composite;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;
import com.mobilesorcery.sdk.ui.launch.IEmulatorLaunchConfigurationPart;

public class EmulatorLaunchConfigurationPartProxy implements IEmulatorLaunchConfigurationPart {

	private IConfigurationElement element;
	private IEmulatorLaunchConfigurationPart delegate;

	public EmulatorLaunchConfigurationPartProxy(IConfigurationElement element) {
		this.element = element;
	}

	@Override
	public void init(ILaunchConfiguration config) throws CoreException {
		initDelegate();
		delegate.init(config);
	}

	@Override
	public Composite createControl(Composite parent, IUpdatableControl updatable) {
		initDelegate();
		return delegate.createControl(parent, updatable);
	}

	@Override
	public void apply(ILaunchConfigurationWorkingCopy copy) {
		initDelegate();
		delegate.apply(copy);
	}

	private void initDelegate() {
		if (element != null) {
			try {
				delegate = (IEmulatorLaunchConfigurationPart) element.createExecutableExtension("part");
			} catch (Exception e) {
				CoreMoSyncPlugin.getDefault().log(e);
			} finally {
				element = null;
			}
		}
	}

	@Override
	public IMessageProvider validate() {
		initDelegate();
		return delegate.validate();
	}

}
