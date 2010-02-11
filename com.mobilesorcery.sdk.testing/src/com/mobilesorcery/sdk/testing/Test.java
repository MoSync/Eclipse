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
