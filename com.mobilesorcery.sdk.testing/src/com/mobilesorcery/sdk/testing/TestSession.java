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
