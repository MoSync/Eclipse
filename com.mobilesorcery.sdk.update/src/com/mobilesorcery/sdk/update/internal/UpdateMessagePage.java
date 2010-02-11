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

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public class UpdateMessagePage extends WizardPage {

	private Document updateMessageDocument;
	private TextViewer updateMessageViewer;
	private String updateMessage;

	protected UpdateMessagePage(String pageName) {
		super(pageName);
	}

	public void setUpdateMessage(String updateMessage) {
		this.updateMessage = updateMessage;
	}

	public void createControl(Composite parent) {

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());

		setTitle(Messages.UpdateMessagePage_Title);
		setMessage(Messages.UpdateMessagePage_Message);

		updateMessageDocument = new Document(updateMessage);
		updateMessageViewer = new TextViewer(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.WRAP);
		updateMessageViewer.setInput(updateMessageDocument);
		updateMessageViewer.setEditable(false);
		updateMessageViewer.getTextWidget().getCaret().setVisible(false);

		updateMessageViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		setControl(composite);
	}

	public boolean canFinish() {
		return isCurrentPage();
	}

}