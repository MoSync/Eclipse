package com.mobilesorcery.sdk.testing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class TestResult extends TestListenerBase {

	public static final int TIME_UNDEFINED = -1;
	
	private HashMap<ITest, List<Object>> failures = new HashMap<ITest, List<Object>>();
	private HashSet<ITest> inProgress = new HashSet<ITest>();
	private HashSet<ITest> done = new HashSet<ITest>();
	private HashMap<ITest, Integer> elapsedTime = new HashMap<ITest, Integer>();
	private HashMap<ITest, HashMap<String, Object>> properties = new HashMap<ITest, HashMap<String, Object>>();
	
	private ITestSession session;
	
	/**
	 * Adds a failure to a test.  
	 * @param test
	 * @param errorToken A language specific representation of the failure,
	 * may be a <code>Throwable</code>, or just an error message <code>String</code>.
	 */
	public synchronized void addFailure(ITest test, Object errorToken) {
		List<Object> failuresForTest = failures.get(test);
		if (failuresForTest == null) {
			failuresForTest = new ArrayList<Object>();
			failures.put(test, failuresForTest);
		}
		
		failuresForTest.add(errorToken);
	}

	public TestResult(ITestSession session) {
		this.session = session;
	}
	
	public void startTest(ITest test) {
		if (!test.isSuite()) {
			inProgress.add(test);
		}
		notifyListeners(new TestSessionEvent(TestSessionEvent.TEST_STARTED, session, test));
	}
	
	public void endTest(ITest test, int elapsedTimeInMillis) {
		if (!test.isSuite()) {
			done.add(test);
			inProgress.remove(test);
			if (elapsedTimeInMillis != TIME_UNDEFINED) {
				elapsedTime.put(test, elapsedTimeInMillis);
			}
		}
		notifyListeners(new TestSessionEvent(TestSessionEvent.TEST_FINISHED, session, test));
	}
	
	public int countTestsInProgress() {
		return inProgress.size();		
	}
	
	public int countRunTests() {
		return done.size(); 
	}

	public int countFailedTests() {
		return failures.size();
	}

	public boolean passed(ITest test, boolean recursive) {
		List<Object> failuresForTest = failures.get(test);
		boolean passed = failuresForTest == null || failuresForTest.size() == 0;
		
		if (recursive && passed && test instanceof ITestSuite) {
			ITestSuite suite = (ITestSuite) test;
			ITest[] tests = suite.getTests();
			for (int i = 0; i < tests.length; i++) {
				if (!passed(tests[i], true)) {
					return false;
				}
			}
		}
		
		return passed;
	}
	
	public boolean hasFinished(ITest test) {
		if (test.isSuite()) {
			for (ITest oneTest : ((ITestSuite)test).getTests()) {
				if (!hasFinished(oneTest)) {
					return false;
				}
			}
			return true;
		} else {
			return done.contains(test);
		}
	}
	
	public boolean isRunning(ITest test) {
		if (test.isSuite()) {
			for (ITest oneTest : ((ITestSuite)test).getTests()) {
				if (isRunning(oneTest)) {
					return true;
				}
			}
			
			return false;
		} else {
			return inProgress.contains(test);
		}
	}

	public void setProperty(ITest test, String key, Object value) {
		HashMap<String, Object> properties = this.properties.get(test);
		if (properties == null) {
			properties = new HashMap<String, Object>();
			this.properties.put(test, properties);
		}
		
		if (value == null) {
			properties.remove(key);
		} else {
			properties.put(key, value);
		}
	}
	
	public Object getProperty(ITest test, String key) {
		HashMap<String, Object> propertiesForTest = properties.get(test);
		return propertiesForTest == null ? null : propertiesForTest.get(key);
	}

	
	public List<Object> getFailures(ITest test) {
		return failures.get(test);
	}

	public int getElapsedTime(ITest test) {
		if (test.isSuite()) {
			int testsWithElapsedTime  = 0;
			int aggElapsedTime = 0;
			ITestSuite suite = (ITestSuite) test;
			for (ITest oneTest : suite.getTests()) {
				int elapsedTimeForOneTest = getElapsedTime(oneTest);
				if (elapsedTimeForOneTest != TIME_UNDEFINED) {
					aggElapsedTime += getElapsedTime(oneTest);
					testsWithElapsedTime++;
				}
			}
			
			return testsWithElapsedTime == 0 ? TIME_UNDEFINED : aggElapsedTime;
		} else {
			Integer elapsedTime = this.elapsedTime.get(test);
			return elapsedTime == null ? TIME_UNDEFINED : elapsedTime.intValue();
		}		
	}


}
