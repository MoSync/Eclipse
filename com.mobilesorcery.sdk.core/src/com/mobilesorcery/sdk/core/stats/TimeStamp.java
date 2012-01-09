package com.mobilesorcery.sdk.core.stats;

public class TimeStamp extends DecimalVariable {

	public static final String TYPE = "t";

	public void set() {
		set(System.currentTimeMillis());
	}

	@Override
	public String getType() {
		return TYPE;
	}


}
