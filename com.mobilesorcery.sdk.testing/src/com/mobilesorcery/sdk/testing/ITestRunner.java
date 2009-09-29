package com.mobilesorcery.sdk.testing;

/**
 * A generic interface for running tests in
 * a method-agnostic way.
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 *
 */
public interface ITestRunner {

	/**
	 * <p>Starts the tests - the <code>ITestRunner</code>
	 * should add <code>ITest</code>s to the session.</p>
	 * <p>This method should never be called directly
	 * by clients, instead <code>ITestSession.start()</code>
	 * should be called.<p> 
	 * @param session
	 */
	public void run(ITestSession session);
}
