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

import org.eclipse.jdt.internal.junit.ui.CounterPanel;
import org.eclipse.jdt.internal.junit.ui.JUnitProgressBar;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;

import com.mobilesorcery.sdk.testing.ITest;
import com.mobilesorcery.sdk.testing.ITestSession;
import com.mobilesorcery.sdk.testing.ITestSessionListener;
import com.mobilesorcery.sdk.testing.ITestSuite;
import com.mobilesorcery.sdk.testing.TestManager;
import com.mobilesorcery.sdk.testing.TestResult;
import com.mobilesorcery.sdk.testing.TestSessionEvent;

public class TestRunComposite extends Composite implements ITestSessionListener {

	private TreeViewer testLog;
	private TableViewer failureDetails;
	private TestSessionLabelProvider testSessionsLabelProvider;
	private CounterPanel counter;
	private JUnitProgressBar progress;
	private ITestSession currentTestSession;
	private FailureDetailsContentProvider failureDetailsContentProvider;

	public TestRunComposite(Composite parent, int style) {
		super(parent, style);
		init();

		TestManager.getInstance().addSessionListener(this);

		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				TestManager.getInstance().removeSessionListener(
						TestRunComposite.this);
			}
		});
	}

	private void init() {
		setLayout(new GridLayout(1, false));
		counter = new CounterPanel(this);
		counter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		progress = new JUnitProgressBar(this);
		progress.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		testLog = new TreeViewer(this, SWT.BORDER | SWT.SINGLE);
		testLog.setContentProvider(new TestSessionContentProvider());
		testSessionsLabelProvider = new TestSessionLabelProvider();
		testLog.setLabelProvider(testSessionsLabelProvider);

		testLog.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		
		failureDetails = new TableViewer(this, SWT.BORDER | SWT.SINGLE);
		failureDetailsContentProvider = new FailureDetailsContentProvider();
		failureDetails.setContentProvider(failureDetailsContentProvider);
		failureDetails.setLabelProvider(new FailureDetailsLabelProvider());

		failureDetails.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		
		testLog.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				Object selectedTest = selection.getFirstElement();
				failureDetails.setInput(selectedTest == null ? FailureDetailsContentProvider.EMPTY : selectedTest);
			}
		});
	}

	public void handleEvent(final TestSessionEvent event) {
		getDisplay().asyncExec(new Runnable() {
			public void run() {
				handleEventUI(event);
			}
		});
	}

	public void setCurrentTestSession(final ITestSession session) {
		this.currentTestSession = session;
		getDisplay().asyncExec(new Runnable() {
			public void run() {
				resetTestLog(session);
			}
		});		
	}

	protected void handleEventUI(final TestSessionEvent event) {
		ITestSession session = event.session;
		if (this.currentTestSession == session) {
			switch (event.type) {
			case TestSessionEvent.SESSION_STARTED:
				resetTestLog(event.session);
			case TestSessionEvent.TEST_DEFINED:
			case TestSessionEvent.TEST_FINISHED:
			case TestSessionEvent.TEST_FAILED:
			case TestSessionEvent.SESSION_FINISHED:
				ITestSuite suite = event.test.getParentSuite();
				updateTree(session);
				/*if (suite != null) {
					updateTree(suite);
				} else {
					updateTree(event.test);
				}*/
				updateCounter(session);
				break;
			default:
				// Do nothing.
			}

			updateTree(event.test);
		}
	}

	private void updateTree(ITest test) {
		testLog.refresh(test, true);
	}

	private void updateCounter(ITestSession session) {
		int testCount = session.getTestCount(true);
		TestResult testResult = session.getTestResult();
		counter.setTotal(testCount);
		int runValue = testResult.didTestExecutionFail() ? testCount : testResult.countRunTests();
		counter.setRunValue(runValue, 0);
		progress.setMaximum(testCount);
		progress.reset(testResult.countFailedTests() > 0,
				session.getState() == ITestSession.STOPPED, runValue, testCount);
	}

	protected void resetTestLog(ITestSession session) {
		this.currentTestSession = session;
		testSessionsLabelProvider.setTestSession(session);
		failureDetailsContentProvider.setTestSession(session);
		if (session != null) {
			testLog.setInput(session);
			failureDetails.setInput(session);
			updateCounter(session);
		}
	}

	public ISelectionProvider getSelectionProvider() {
		return testLog;
	}

	public ITestSession getTestSession() {
		return currentTestSession;
	}

	public void expandAll() {
		testLog.expandAll();
	}

	public TreeViewer getTreeViewer() {
		return testLog;
	}
}
