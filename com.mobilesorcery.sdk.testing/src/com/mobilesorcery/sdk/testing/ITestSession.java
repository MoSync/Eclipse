package com.mobilesorcery.sdk.testing;

public interface ITestSession extends ITestSuite {
	
	public static final int INIT = 0;
	public static final int RUNNING = 1;
	public static final int FINISHED = 2;
	public static final int STOPPED = 3;
	
	void addSessionListener(ITestSessionListener listener);

	void removeSessionListener(ITestSessionListener listener);

	TestResult getTestResult();

	int getState();

	/**
	 * Starts the test session and runs the tests.
	 */
	void start();

	/**
	 * Ends this test session and disposes of any
	 * allocated resources.
	 */
	void finish();

	/**
	 * Returns the runner that was used to create this session
	 * @return
	 */
	//ITestRunner getRunner();
}
