package com.mobilesorcery.sdk.builder.android.launch;

public class Target {

	private final String id;
	private final int apiLevel;

	Target(String id, int apiLevel) {
		this.id = id;
		this.apiLevel = apiLevel;
	}

	public String getId() {
		return id;
	}

	public int getAPILevel() {
		return apiLevel;
	}
}
