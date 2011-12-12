package com.mobilesorcery.sdk.core;

public enum CapabilityFragmentation {

	BUILDTIME, RUNTIME;

	public boolean matches(String state) {
		return this.toString().toLowerCase().equals(state.toLowerCase());
	}

	public static CapabilityFragmentation create(String str) {
		for (CapabilityFragmentation value : CapabilityFragmentation.values()) {
			if (value.matches(str)) {
				return value;
			}
		}
		return null;
	}
}
