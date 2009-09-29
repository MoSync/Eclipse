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