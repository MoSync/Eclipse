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


public class TestSession extends TestSuite implements ITestSession {

	private TestResult result;
	private ITestSessionListener internalListener;

	private int state = INIT;
	
	public TestSession(String name) {
		super(name);
		internalListener = new ITestSessionListener() {
			public void handleEvent(TestSessionEvent event) {
				notifyListeners(event);
			}
		};
		createNewTestResult();
	}
	
	protected void createNewTestResult() {
		if (result != null) {
			result.removeSessionListener(internalListener);
		}
		result = new TestResult(this);
		result.addSessionListener(internalListener);
	}

	public void start() {
		start(true);
	}
	
	public void start(boolean sync) {
		checkStartable();
		notifyListeners(new TestSessionEvent(TestSessionEvent.SESSION_STARTED, this, this));	

		if (sync) {
			syncStart();
		} else {
			Thread testThread = new Thread(new Runnable() {
				public void run() {
					syncStart();
				}			
			}, "Unit test thread");
			
			testThread.setDaemon(true);
			testThread.start();
		}
	}
	
	protected void checkStartable() {
		if (state != INIT) {
			throw new IllegalStateException("Session already started - use restart instead");
		}
	}

	protected final void syncStart() {
		state = RUNNING;
		try {
			run(result);
		} finally {
			result.removeSessionListener(internalListener);
			finish();
		}		
	}
	
	public TestResult getTestResult() {
		return result;
	}

	public int getState() {
		return state;
	}

	public void finish() {
		if (state != FINISHED) {
			state = FINISHED;
			notifyListeners(new TestSessionEvent(TestSessionEvent.SESSION_FINISHED, this, this));
		}
	}
	


}
