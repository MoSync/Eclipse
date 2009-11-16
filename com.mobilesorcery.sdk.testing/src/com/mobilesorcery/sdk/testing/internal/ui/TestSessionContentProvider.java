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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.mobilesorcery.sdk.testing.ITest;
import com.mobilesorcery.sdk.testing.ITestSession;
import com.mobilesorcery.sdk.testing.ITestSuite;

public class TestSessionContentProvider implements ITreeContentProvider {

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof ITestSession) {
			ITestSession session = (ITestSession) parentElement;
			List<Object> testExecutionFailures = session.getTestResult().getFailures(session);
			ArrayList result = new ArrayList();
			if (testExecutionFailures != null) {
				result.addAll(testExecutionFailures);
			}
			result.addAll(Arrays.asList(((ITestSession) parentElement).getTests()));
			return result.toArray();
		} else if (parentElement instanceof ITestSuite) {
			return ((ITestSuite) parentElement).getTests();
		}
		return new Object[0];		
	}

	public Object getParent(Object element) {
		if (element instanceof ITest) {
			return ((ITest) element).getParentSuite();
		}
		
		return null;
	}

	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}


}
