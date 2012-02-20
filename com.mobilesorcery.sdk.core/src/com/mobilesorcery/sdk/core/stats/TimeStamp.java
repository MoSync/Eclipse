package com.mobilesorcery.sdk.core.stats;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.simple.JSONObject;

public class TimeStamp extends SimpleVariable {

	private final static DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ");

	private long utc;

	public static final String TYPE = "t";

	public void set() {
		this.utc = System.currentTimeMillis();
	}

	public void set(long utc) {
		this.utc = utc;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public void read(JSONObject input) {
		String value = readValue(input);
		try {
			utc = FORMAT.parse(value).getTime();
		} catch (Exception e) {
			// Ignore; utc = 0;
		}
	}

	@Override
	public String toString() {
		return FORMAT.format(new Date(utc));
	}

}
