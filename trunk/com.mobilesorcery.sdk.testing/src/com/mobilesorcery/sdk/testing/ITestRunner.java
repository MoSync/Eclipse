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
