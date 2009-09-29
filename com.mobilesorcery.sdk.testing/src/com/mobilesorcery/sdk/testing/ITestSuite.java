package com.mobilesorcery.sdk.testing;

public interface ITestSuite extends ITest {

	/**
	 * Adds a test to this suite
	 * @param test
	 */
	void addTest(ITest test);
	
	/**
	 * Returns all the children, or subtests, of this test suite
	 * @return
	 */
	ITest[] getTests();

	/**
	 * Returns the number of children, or subtests, of this test suite
	 * @return The number of immediate children of this test suite
	 * @see #getTestCount(boolean)
	 */
	public int getTestCount();
	
	public int getTestCount(boolean recursive);

}
