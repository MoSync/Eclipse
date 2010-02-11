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
/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;


import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.mobilesorcery.sdk.testing.TestPlugin;

/**
 * A panel with counters for the number of Runs, Errors and Failures.
 */
public class CounterPanel extends Composite {
	protected Text fNumberOfErrors;
	protected Text fNumberOfFailures;
	protected Text fNumberOfRuns;
	protected int fTotal;
	protected int fIgnoredCount;

	private Image fErrorIcon;

	public CounterPanel(Composite parent) {
		super(parent, SWT.WRAP);
		fErrorIcon = TestPlugin.getDefault().getImageRegistry().get(TestPlugin.ERROR_IMAGE);
		GridLayout gridLayout= new GridLayout();
		gridLayout.numColumns= 9;
		gridLayout.makeColumnsEqualWidth= false;
		gridLayout.marginWidth= 0;
		setLayout(gridLayout);

		fNumberOfRuns= createLabel("Runs", null, " 0/0  "); //$NON-NLS-1$
		fNumberOfErrors= createLabel("Failures", fErrorIcon, " 0 "); //$NON-NLS-1$
		//fNumberOfFailures= createLabel(JUnitMessages.CounterPanel_label_failures, fFailureIcon, " 0 "); //$NON-NLS-1$

		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				disposeIcons();
			}
		});
	}

	private void disposeIcons() {
		//fErrorIcon.dispose();
		//fFailureIcon.dispose();
	}

	private Text createLabel(String name, Image image, String init) {
		Label label= new Label(this, SWT.NONE);
		if (image != null) {
			image.setBackground(label.getBackground());
			label.setImage(image);
		}
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

		label= new Label(this, SWT.NONE);
		label.setText(name);
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
		//label.setFont(JFaceResources.getBannerFont());

		Text value= new Text(this, SWT.READ_ONLY);
		value.setText(init);
		// bug: 39661 Junit test counters do not repaint correctly [JUnit]
		value.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		value.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING));
		return value;
	}

	public void reset() {
		setErrorValue(0);
		setFailureValue(0);
		setRunValue(0, 0);
		fTotal= 0;
	}

	public void setTotal(int value) {
		fTotal= value;
	}

	public int getTotal(){
		return fTotal;
	}

	public void setRunValue(int value, int ignoredCount) {
		String runString;
		if (ignoredCount == 0)
			runString= MessageFormat.format("{0}/{1}", Integer.toString(value), Integer.toString(fTotal));
		else
			runString= MessageFormat.format("{0}/{1} ({2} ignored)", Integer.toString(value), Integer.toString(fTotal), Integer.toString(ignoredCount));
		fNumberOfRuns.setText(runString);

		if (fIgnoredCount == 0 && ignoredCount > 0	|| fIgnoredCount != 0 && ignoredCount == 0) {
			layout();
		} else {
			fNumberOfRuns.redraw();
			redraw();
		}
		fIgnoredCount= ignoredCount;
	}

	public void setErrorValue(int value) {
		fNumberOfErrors.setText(Integer.toString(value));
		redraw();
	}

	public void setFailureValue(int value) {
		fNumberOfFailures.setText(Integer.toString(value));
		redraw();
	}
}
