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

import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Image;

import com.mobilesorcery.sdk.testing.ITest;
import com.mobilesorcery.sdk.testing.ITestSession;
import com.mobilesorcery.sdk.testing.TestPlugin;
import com.mobilesorcery.sdk.testing.TestResult;

public class TestSessionLabelProvider extends StyledCellLabelProvider {

	private ITestSession session;
	private NumberFormat timeFormat;

	public TestSessionLabelProvider() {
		timeFormat = NumberFormat.getNumberInstance();
		timeFormat.setGroupingUsed(true);
		timeFormat.setMinimumFractionDigits(3);
		timeFormat.setMaximumFractionDigits(3);
		timeFormat.setMinimumIntegerDigits(1);
	}
	
	public void setTestSession(ITestSession session) {
		this.session = session;		
	}
	
	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		String mainText = getText(element);
		StyledString text = new StyledString(mainText);
		String elapsedTimeText = getElapsedTimeText(element);
		if (elapsedTimeText != null) {
			text = StyledCellLabelProvider.styleDecoratedString(MessageFormat.format("{0} {1}", text, elapsedTimeText), StyledString.COUNTER_STYLER, text);
		}
		cell.setText(text.getString());
		cell.setStyleRanges(text.getStyleRanges());
		Image image = getImage(element);
		cell.setImage(image);	
	}

	private String getElapsedTimeText(Object element) {
		if (element instanceof ITest && session != null) {
			ITest test = (ITest) element;
			if (session.getTestResult().hasFinished(test)) {
				int elapsedTimeInMs = session.getTestResult().getElapsedTime((ITest) element);
				if (elapsedTimeInMs != TestResult.TIME_UNDEFINED) {
					return MessageFormat.format("{0} s", timeFormat.format(((float) elapsedTimeInMs) / 1000));
				}
			}
		}
		
		return null;
	}

	private String getText(Object element) {
		if (element instanceof ITest) {
			String name = ((ITest) element).getName();
			return name == null ? "?" : name;
		}
		return element.toString(); //$NON-NLS-1$
	}

	private Image getImage(Object element) {
		String imageKey = null;

		if (element instanceof Exception) {
			imageKey = TestPlugin.TEST_SUITE_ERROR_IMAGE;
		} else  if (element instanceof ITest) {
			ITest test = (ITest) element;
			if (session != null) {
				boolean hasFinished = session.getTestResult().hasFinished(test);
				boolean passed = session.getTestResult().passed(test, true);
				boolean isRunning = session.getTestResult().isRunning(test);
				boolean isSuite = test.isSuite();
								
				if (isSuite) {
					if (isRunning) {
						imageKey = TestPlugin.TEST_SUITE_RUNNING_IMAGE; 
					} else if (hasFinished) {
						if (passed) {
							imageKey = TestPlugin.TEST_SUITE_OK_IMAGE;
						} else {
							imageKey = TestPlugin.TEST_SUITE_ERROR_IMAGE;
						}
					} else {
						imageKey = TestPlugin.TEST_SUITE_IMAGE;
					}
				} else {
					if (isRunning) {
						imageKey = TestPlugin.TEST_SUITE_RUNNING_IMAGE; 
					} else if (hasFinished) {
						if (passed) {
							imageKey = TestPlugin.TEST_OK_IMAGE;
						} else {
							imageKey = TestPlugin.TEST_ERROR_IMAGE;
						}
					} else {
						imageKey = TestPlugin.TEST_IMAGE;
					}
				}
				
				
			}			
		}

		if (imageKey != null) {
			return TestPlugin.getDefault().getImageRegistry().get(imageKey);
		} else {
			return null;
		}
	}
}
