package com.mobilesorcery.sdk.core;

public enum CapabilityState {

	SUPPORTED, UNSUPPORTED, NOT_IMPLEMENTED, REQUIRES_PERMISSION, REQUIRES_PRIVILEGED_PERMISSION;

	public boolean matches(String state) {
		return state != null && this.toString().toLowerCase().equals(state.toLowerCase());
	}

	public static CapabilityState create(String str) {
		for (CapabilityState value : CapabilityState.values()) {
			if (value.matches(str)) {
				return value;
			}
		}
		return null;
	}

}