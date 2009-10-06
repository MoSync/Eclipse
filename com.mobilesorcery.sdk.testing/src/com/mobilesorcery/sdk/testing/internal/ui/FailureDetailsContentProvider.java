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
package com.mobilesorcery.sdk.testing.internal.ui;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.mobilesorcery.sdk.testing.ITest;
import com.mobilesorcery.sdk.testing.ITestSession;

public class FailureDetailsContentProvider implements IStructuredContentProvider {

	public static final Object EMPTY = new Object();
	
	private ITestSession session;

	public void setTestSession(ITestSession session) {
		this.session = session;
	}
	
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof ITest && session != null) {
			List<Object> failures = session.getTestResult().getFailures((ITest) inputElement);
			if (failures != null) {
				return failures.toArray();
			}
		}
		return new Object[0];
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

}
