/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
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
