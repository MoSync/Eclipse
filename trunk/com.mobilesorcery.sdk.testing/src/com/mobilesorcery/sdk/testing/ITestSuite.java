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
