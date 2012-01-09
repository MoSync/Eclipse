package com.mobilesorcery.sdk.core.stats;


public class CounterVariable extends DecimalVariable {

	public static final String TYPE = "c";

	public synchronized void inc() {
		value.incrementAndGet();
	}

	public synchronized void dec() {
		value.decrementAndGet();
	}

	@Override
	public String getType() {
		return TYPE;
	}

}
