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

import java.text.MessageFormat;

public class AssertionFailed implements IAssertionFailed {

	private Object expected;
	private Object actual;

	public AssertionFailed(Object expected, Object actual) {
		this.expected = expected;
		this.actual = actual;
	}

	public Object actual() {
		return actual;
	}

	public Object expected() {
		return expected;
	}

	public String toString() {
		return toString(this);
	}

	public static String toString(IAssertionFailed assertionFailed) {
		return MessageFormat.format("Excepted {0}. Actual: {1}.", assertionFailed.expected(), assertionFailed.actual());
	}
	
}
