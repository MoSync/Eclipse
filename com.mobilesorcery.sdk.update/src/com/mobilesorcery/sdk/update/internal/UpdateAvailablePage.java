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
package com.mobilesorcery.sdk.update.internal;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class UpdateAvailablePage extends WizardPage {

	protected UpdateAvailablePage(String pageName) {
		super(pageName);
	}

	public void createControl(Composite parent) {

		Composite composite = new Composite(parent, SWT.NONE);

		composite.setLayout(new GridLayout());

		Label message = new Label(composite, SWT.None);
		message.setText(Messages.UpdateAvailablePage_Message);
		message.setLayoutData(new GridData());
		setTitle(Messages.UpdateAvailablePage_Title);
		setControl(composite);
	}

}