package com.mobilesorcery.sdk.builder.java.capabilities;

import java.util.ArrayList;

import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.filter.IDeviceFilterFactory;

public class JavaMEOpCertFilterFactory implements IDeviceFilterFactory {

	public IDeviceFilter createFilter(IMemento memento) {
		IMemento[] requestedCapabilityMementos = memento.getChildren("requested-capability");
		ArrayList<String> requestedCapabilities = new ArrayList<String>();
		for (IMemento requestedCapabilityMemento : requestedCapabilityMementos) {
			String id = requestedCapabilityMemento.getString("id");
			if (id != null) {
				requestedCapabilities.add(id);
			}
		}
	
		return new JavaMEOpCertFilter(requestedCapabilities.toArray(new String[0]));
	}

}
