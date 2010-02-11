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

public class TestSessionEvent {

	public static final int SESSION_STARTED = 1;
	public static final int SESSION_FINISHED = 2;
	public static final int TEST_STARTED = 3;
	public static final int TEST_FINISHED = 4;
	public static final int TEST_DEFINED = 5;
	public static final int TEST_FAILED = 6;	
	
	public int type;
	public ITestSession session;
	public ITest test;

	public TestSessionEvent(int type, ITestSession session, ITest test) {
		this.type = type;
		this.session = session;
		this.test = test;
	}

}
