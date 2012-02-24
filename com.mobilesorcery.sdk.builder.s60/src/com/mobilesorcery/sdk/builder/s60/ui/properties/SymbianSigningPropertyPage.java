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
package com.mobilesorcery.sdk.builder.s60.ui.properties;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;

import com.mobilesorcery.sdk.builder.s60.PropertyInitializer;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.ui.MoSyncPropertyPage;
import com.mobilesorcery.sdk.ui.PasswordTextFieldDecorator;

public class SymbianSigningPropertyPage extends MoSyncPropertyPage implements IWorkbenchPropertyPage {

    private Button useProjectSpecific;
    private FileFieldEditor certFile;
    private FileFieldEditor keyFile;
    private Group signingGroup;
    private Text passkey;
    private Label passkeyLabel;
    private PasswordTextFieldDecorator passkeyDec;

    public SymbianSigningPropertyPage() {
    	super(true);
    }

    @Override
	protected Control createContents(Composite parent) {
        Composite main = new Composite(parent, SWT.NONE);
        main.setLayout(new GridLayout(1, false));

        useProjectSpecific = new Button(main, SWT.CHECK);
        useProjectSpecific.setText(Messages.SymbianSigningPropertyPage_EnableProjectSpecific);

        useProjectSpecific.setSelection(PropertyUtil.getBoolean(getProject(), PropertyInitializer.S60_PROJECT_SPECIFIC_KEYS));
        useProjectSpecific.addListener(SWT.Selection, new Listener() {
            @Override
			public void handleEvent(Event event) {
                updateUI();
            }
        });

        Label separator = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        signingGroup = new Group(main, SWT.NONE);
        signingGroup.setText(Messages.SymbianSigningPropertyPage_Signing);
        signingGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        keyFile = new FileFieldEditor("dummy.1", Messages.SymbianSigningPropertyPage_KeyFile, signingGroup); //$NON-NLS-1$
        keyFile.setPage(this);
        certFile = new FileFieldEditor("dummy.2", Messages.SymbianSigningPropertyPage_CertificateFile, signingGroup); //$NON-NLS-1$
        certFile.setPage(this);

        String keyFilePath = getProject().getProperty(PropertyInitializer.S60_KEY_FILE);
        keyFile.setStringValue(keyFilePath == null ? "" : keyFilePath); //$NON-NLS-1$

        String certFilePath = getProject().getProperty(PropertyInitializer.S60_CERT_FILE);
        certFile.setStringValue(certFilePath == null ? "" : certFilePath); //$NON-NLS-1$

        passkeyLabel = new Label(signingGroup, SWT.NONE);
        passkeyLabel.setText(Messages.SymbianSigningPropertyPage_Passkey);

        passkey = new Text(signingGroup, SWT.BORDER | SWT.SINGLE);
        passkeyDec = new PasswordTextFieldDecorator(passkey);
        GridData passkeyData = new GridData(GridData.FILL_HORIZONTAL);
        passkeyData.horizontalSpan = 2;
        passkey.setLayoutData(passkeyData);

        String passKeyValue = getProject().getProperty(PropertyInitializer.S60_PASS_KEY);
        passkey.setText(passKeyValue == null ? "" : passKeyValue);

        setMessage("The passkey will be stored as clear text", IMessageProvider.WARNING);

        updateUI();

        return main;
    }

    @Override
	public void updateUI() {
        keyFile.setEnabled(useProjectSpecific.getSelection(), signingGroup);
        certFile.setEnabled(useProjectSpecific.getSelection(), signingGroup);
        passkeyLabel.setEnabled(useProjectSpecific.getSelection());
        passkeyDec.setEnabled(useProjectSpecific.getSelection());

        if (!useProjectSpecific.getSelection()) {
            keyFile.setStringValue(getProject().getDefaultProperty(PropertyInitializer.S60_KEY_FILE));
            certFile.setStringValue(getProject().getDefaultProperty(PropertyInitializer.S60_CERT_FILE));
            passkey.setText(getProject().getDefaultProperty(PropertyInitializer.S60_PASS_KEY));
        }
    }

    @Override
	public boolean performOk() {
        PropertyUtil.setBoolean(getProject(), PropertyInitializer.S60_PROJECT_SPECIFIC_KEYS, useProjectSpecific.getSelection());
        getProject().setProperty(PropertyInitializer.S60_KEY_FILE, useProjectSpecific.getSelection() ? keyFile.getStringValue() : null);
        getProject().setProperty(PropertyInitializer.S60_CERT_FILE, useProjectSpecific.getSelection() ? certFile.getStringValue() : null);
        getProject().setProperty(PropertyInitializer.S60_PASS_KEY, useProjectSpecific.getSelection() ? passkey.getText() : null);

        return true;
    }

    @Override
	public void performDefaults() {
    	useProjectSpecific.setSelection(PropertyUtil.toBoolean(getProject().getDefaultProperty(PropertyInitializer.S60_PROJECT_SPECIFIC_KEYS)));
    	keyFile.setStringValue(getProject().getDefaultProperty(PropertyInitializer.S60_KEY_FILE));
    	certFile.setStringValue(getProject().getDefaultProperty(PropertyInitializer.S60_CERT_FILE));
    	setText(passkey, getProject().getDefaultProperty(PropertyInitializer.S60_PASS_KEY));
    	updateUI();
    }

}
