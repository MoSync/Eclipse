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

import java.util.ArrayList;

public class TestSuite extends Test implements ITestSuite {

	protected ArrayList<ITest> tests = new ArrayList<ITest>();

	public TestSuite(String name) {
		super(name);
	}
	
	public int getTestCount() {
		return tests.size();
	}

	public int getTestCount(boolean recursive) {
		if (recursive) {
			int total = 0;
			for (ITest test : tests) {
				if (test instanceof ITestSuite) {
					ITestSuite suite = (ITestSuite) test;
					total += suite.getTestCount();
				}
			}
			
			return total;
		} else {
			return getTestCount();
		}
	}
	
	public ITest[] getTests() {
		return tests.toArray(new ITest[0]);
	}

	public void addTest(ITest test) {
		if (test == this) {
			return;
		}
		tests.add(test);
		test.setParentSuite(this);
	}
	
	public void run(TestResult result) {
		for (ITest test : tests) {
			// Only client timing support as of now.
			long startTime = System.currentTimeMillis();
			try {
				result.startTest(test);
				test.run(result);				
			} catch (Throwable e) {
				result.addFailure(test, e);
			} finally {
				int elapsedTime = (int) (System.currentTimeMillis() - startTime);
				result.endTest(test, elapsedTime);
			}
		}
	}
	
	public boolean isSuite() {
		return true;
	}

}
