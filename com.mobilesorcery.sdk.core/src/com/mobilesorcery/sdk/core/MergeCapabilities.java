package com.mobilesorcery.sdk.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MergeCapabilities implements ICapabilities {

	private final ICapabilities[] children;

	public MergeCapabilities(ICapabilities... children) {
		this.children = children;
	}

	@Override
	public boolean hasCapability(String capability) {
		return getCapability(capability) != null;
	}

	@Override
	public Set<ICapability> listCapabilities() {
		HashSet<ICapability> result = new HashSet<ICapability>();
		for (int i = 0; i < children.length; i++) {
			if (children[i] != null) {
				result.addAll(children[i].listCapabilities());
			}
		}
		return result;
	}

	@Override
	public ICapability getCapability(String capability) {
		for (int i = 0; i < children.length; i++) {
			ICapability child = children[i] == null ? null : children[i].getCapability(capability);
			if (child != null) {
				return child;
			}
		}

		return null;
	}

	@Override
	public String toString() {
		return Arrays.asList(children).toString();
	}

}