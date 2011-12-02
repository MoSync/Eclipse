package com.mobilesorcery.sdk.core;

public enum CapabilityState {

	SUPPORTED, UNSUPPORTED, NOT_IMPLEMENTED, REQUIRES_PERMISSION, REQUIRES_PRIVILEGED_PERMISSION;

	public boolean matches(String state) {
		return this.toString().toLowerCase().equals(state.toLowerCase());
	}

}