package com.mobilesorcery.sdk.capabilities.devices;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.filter.IDeviceFilterFactory;

public class DeviceCapabilitiesFilterFactory implements IDeviceFilterFactory  {

	@Override
	public IDeviceFilter createFilter(IMemento child) {
		String requestedCapability = child.getString("capability");
		String stateStr = child.getString("state");
		try {
			CapabilityState state = CapabilityState.valueOf(stateStr);
			return new DeviceCapabilitiesFilter(requestedCapability, state);
		} catch (Exception e) {
			return null;
		}
	}

	

}
