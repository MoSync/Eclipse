package com.mobilesorcery.sdk.core.stats;

import org.json.simple.JSONObject;

public abstract class SimpleVariable implements IVariable {

	@Override
	public void write(JSONObject output) {
		output.put("value", toString());
	}

	protected String readValue(JSONObject input) {
		return (String) input.get("value");
	}
}
