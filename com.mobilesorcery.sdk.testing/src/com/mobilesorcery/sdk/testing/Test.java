package com.mobilesorcery.sdk.testing;

public class Test extends TestListenerBase implements ITest {

	private String name;
	private ITestSuite suite;

	public Test(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	/**
	 * Clients should override this.
	 */
	public void run(TestResult result) throws Throwable {
	}

	public boolean isSuite() {
		return false;
	}

	public void setParentSuite(ITestSuite suite) {
		this.suite = suite;
	}
	
	public ITestSuite getParentSuite() {
		return suite;
	}

}
