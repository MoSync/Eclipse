package com.mobilesorcery.sdk.core;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import com.mobilesorcery.sdk.core.ICapabilities;
import com.mobilesorcery.sdk.core.Util;


public class Capabilities implements ICapabilities {

	private final HashMap<String, ICapability> capabilites = new HashMap<String, ICapability>();

	public Capabilities() {
	}

	public void setCapability(ICapability capability) {
		capabilites.put(capability.getName(), capability);
	}

	public void removeCapability(String name) {
		capabilites.remove(name);
	}

	public ICapabilities merge(ICapabilities other) {
		return new MergeCapabilities(this, other);
	}

	public void copyMerge(ICapabilities other) {
		for (ICapability capability : other.listCapabilities()) {
			if (!hasCapability(capability.getName())) {
				setCapability(other.getCapability(capability.getName()));
			}
		}
	}

	@Override
	public boolean hasCapability(String capability) {
		return getCapability(capability) != null;
	}

	@Override
	public Set<ICapability> listCapabilities() {
		return new TreeSet<ICapability>(capabilites.values());
	}

	@Override
	public ICapability getCapability(String capability) {
		ICapability result = capabilites.get(capability);
		if (result == null) {
			return capability == null ? null : getCapability(Util.getParentKey(capability));
		} else {
			return result;
		}
	}

	@Override
	public String toString() {
		return capabilites.toString();
	}

}
