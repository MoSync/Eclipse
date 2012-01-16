package com.mobilesorcery.sdk.core.stats;

import java.util.concurrent.atomic.AtomicLong;

import org.json.simple.JSONObject;

public class DecimalVariable extends SimpleVariable {

	public static final String TYPE = "n";

	protected AtomicLong value = new AtomicLong();

	public void set(long value) {
		this.value.set(value);
	}

	public long get() {
		return value.longValue();
	}

	@Override
	public String toString() {
		return Long.toString(value.longValue());
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public void read(JSONObject o) {
		value.set(Long.parseLong(readValue(o)));
	}

	@Override
	public void write(JSONObject output) {
		output.put("value", value.longValue());
	}
}
