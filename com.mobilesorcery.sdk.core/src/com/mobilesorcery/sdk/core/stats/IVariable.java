package com.mobilesorcery.sdk.core.stats;

import org.json.simple.JSONObject;

public interface IVariable {
	public void write(JSONObject output);
	public void read(JSONObject input);
	public String getType();
}
