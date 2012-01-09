package com.mobilesorcery.sdk.core.stats;

import org.json.simple.JSONObject;

public class StringVariable extends SimpleVariable {

	static final String TYPE = "s";

	private String value;

	public void set(String value) {
		this.value = value;
	}

	public String get() {
		return value;
	}

	@Override
	public String toString() {
		return value;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public void read(JSONObject o) {
		value = readValue(o);
	}
}
