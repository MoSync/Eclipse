package com.mobilesorcery.sdk.profiles.filter.elementfactories;

import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.filter.DeviceCapabilitiesFilter;
import com.mobilesorcery.sdk.profiles.filter.IDeviceFilterFactory;

public class DeviceCapabilitiesFilterFactory implements IDeviceFilterFactory  {

	public static final String ID = "com.mobilesorcery.sdk.capabilities.devices.elementfactory";

	@Override
	public IDeviceFilter createFilter(IMemento child) {
		IMemento capabilities = child.getChild("capabilities");
		String[] required = new String[0];
		String[] optional = new String[0];
		if (capabilities != null) {
			String requiredStr = capabilities.getString("required");
			if (requiredStr != null) {
				required = PropertyUtil.toStrings(requiredStr);
			}
			String optionalStr = capabilities.getString("optional");
			if (optionalStr != null) {
				optional = PropertyUtil.toStrings(optionalStr);
			}
			return new DeviceCapabilitiesFilter(required, optional);
		}
		return null;
	}
}
