package com.mobilesorcery.sdk.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MergeCapabilities implements ICapabilities {

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