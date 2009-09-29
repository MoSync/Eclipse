package com.mobilesorcery.sdk.testing.internal.ui;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.mobilesorcery.sdk.testing.ITestSession;
import com.mobilesorcery.sdk.testing.ITestSuite;

public class TestSessionContentProvider implements ITreeContentProvider {

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof ITestSession) {
			return ((ITestSession) parentElement).getTests();
		} else if (parentElement instanceof ITestSuite) {
			return ((ITestSuite) parentElement).getTests();
		}
		return new Object[0];		
	}

	public Object getParent(Object element) {
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
