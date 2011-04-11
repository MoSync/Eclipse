package com.mobilesorcery.sdk.capabilities.devices;

import java.text.MessageFormat;

import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.capabilities.core.ICapabilities;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.filter.AbstractDeviceFilter;

public class DeviceCapabilitiesFilter extends AbstractDeviceFilter {

	private final static String FACTORY_ID = "com.mobilesorcery.sdk.capabilities.devices.elementfactory";

	private String requestedCapability;
	private CapabilityState disallowedCapabilityState;

	public DeviceCapabilitiesFilter(String requestedCapability, CapabilityState disallowedCapabilityState) {
		this.requestedCapability = requestedCapability;
		this.disallowedCapabilityState = disallowedCapabilityState;
	}
	
	@Override
	public String getFactoryId() {
		return FACTORY_ID;
	}

	@Override
	public void saveState(IMemento memento) {
		memento.putString("capability", requestedCapability);
		memento.putString("state", disallowedCapabilityState.toString());
	}

	@Override
	public boolean acceptProfile(IProfile profile) {
		ICapabilities profileCapabilites = DeviceCapabilitesPlugin.getDefault().getCapabilitiesForProfile(profile);
		CapabilityState capabilityState = (CapabilityState) profileCapabilites.getCapabilityValue(requestedCapability);
		return disallowedCapabilityState != capabilityState;
	}

	public String toString() {
		// TODO: Much better string ...
		return MessageFormat.format("Requires device capability {0} NOT being {1}", requestedCapability, disallowedCapabilityState);
	}
}
