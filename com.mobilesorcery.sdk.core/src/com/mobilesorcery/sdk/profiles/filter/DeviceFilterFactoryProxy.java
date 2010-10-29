package com.mobilesorcery.sdk.profiles.filter;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;

public class DeviceFilterFactoryProxy implements IDeviceFilterFactory {

	private IConfigurationElement element;
	private IDeviceFilterFactory delegate;

	public DeviceFilterFactoryProxy(IConfigurationElement element) {
		this.element = element;
	}
	
	private void initDelegate() {
		if (this.element != null) {
			try {
				delegate = (IDeviceFilterFactory) element.createExecutableExtension("class");
			} catch (Exception e) {
				CoreMoSyncPlugin.getDefault().log(e);
			}
			element = null;
		}
	}

	public IDeviceFilter createFilter(IMemento child) {
		initDelegate();
		return delegate.createFilter(child);
	}


}
