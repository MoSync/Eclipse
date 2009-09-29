package com.mobilesorcery.sdk.testing;

public interface ITest {

	public String getName();

	public void run(TestResult result) throws Throwable;
	
	public boolean isSuite();
	
	public ITestSuite getParentSuite();

	public void setParentSuite(ITestSuite testSuite);
}
