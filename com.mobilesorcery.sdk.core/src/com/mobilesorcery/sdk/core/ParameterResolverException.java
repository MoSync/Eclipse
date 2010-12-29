package com.mobilesorcery.sdk.core;

public class ParameterResolverException extends Exception {

	private String key;

	public ParameterResolverException(String key, String msg) {
		super("Key " + key + ": " + msg);
		this.key = key;
	}

	public String getKey() {
		return key;
	}
}
