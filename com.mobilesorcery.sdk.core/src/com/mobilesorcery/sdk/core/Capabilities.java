package com.mobilesorcery.sdk.core;

import java.util.HashMap;
import java.util.Set;

import com.mobilesorcery.sdk.core.ICapabilities;
import com.mobilesorcery.sdk.core.Util;


public class Capabilities implements ICapabilities {

	private HashMap<String, Object> capabilites = new HashMap<String, Object>();

	public Capabilities() {
	}
	
	public void setCapability(String capability) {
		setCapability(capability, Boolean.TRUE);
	}
	
	public void setCapability(String capability, Object value) {
		capabilites.put(capability, value);
	}

	public ICapabilities merge(ICapabilities other) {
		return new MergeCapabilities(this, other);
	}
	
	public void copyMerge(ICapabilities other) {
		for (String capability : other.listCapabilities()) {
			if (!hasCapability(capability)) {
				setCapability(capability, other.getCapabilityValue(capability));
			}
		}
	}
	
	public boolean hasCapability(String capability) {
		return getCapabilityValue(capability) != null;
	}

	public Set<String> listCapabilities() {
		return capabilites.keySet();
	}

	public Object getCapabilityValue(String capability) {
		Object result = capabilites.get(capability);
		if (result == null) {
			return capability == null ? null : getCapabilityValue(Util.getParentKey(capability));
		} else {
			return result;
		}
	}

	public String toString() {
		return capabilites.toString();
	}

}
