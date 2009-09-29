package com.mobilesorcery.sdk.testing;

public class TestSessionEvent {

	public static final int SESSION_STARTED = 1;
	public static final int SESSION_FINISHED = 2;
	public static final int TEST_STARTED = 3;
	public static final int TEST_FINISHED = 4;
	public static final int TEST_DEFINED = 5;	
	
	public int type;
	public ITestSession session;
	public ITest test;

	public TestSessionEvent(int type, ITestSession session, ITest test) {
		this.type = type;
		this.session = session;
		this.test = test;
	}

}
