package com.mobilesorcery.sdk.capabilities.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.mobilesorcery.sdk.core.Util;

public class Capabilities implements ICapabilities {

	public static class MergeCapabilities implements ICapabilities {

		private ICapabilities[] children;

		public MergeCapabilities(ICapabilities... children) {
			this.children = children;
		}

		public boolean hasCapability(String capability) {
			return getCapabilityValue(capability) != null;
		}

		public Set<String> listCapabilities() {
			HashSet<String> result = new HashSet<String>();
			for (int i = 0; i < children.length; i++) {
				if (children[i] != null) {
					result.addAll(children[i].listCapabilities());
				}
			}
			return result;
		}

		public Object getCapabilityValue(String capability) {
			for (int i = 0; i < children.length; i++) {
				Object value = children[i] == null ? null : children[i].getCapabilityValue(capability);
				if (value != null) {
					return value;
				}
			}
			
			return null;
		}

		public String toString() {
			return Arrays.asList(children).toString();
		}
	}

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
