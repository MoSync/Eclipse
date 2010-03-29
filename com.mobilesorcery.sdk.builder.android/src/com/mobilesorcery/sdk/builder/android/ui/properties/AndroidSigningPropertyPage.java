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
package com.mobilesorcery.sdk.builder.android.ui.properties;

import org.eclipse.core.resources.IProject;
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
import org.eclipse.ui.dialogs.PropertyPage;

import com.mobilesorcery.sdk.builder.android.PropertyInitializer;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.ui.PasswordTextFieldDecorator;

public class AndroidSigningPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {

    private Button useProjectSpecific;
    private FileFieldEditor keyStore;
    private Group signingGroup;
    private Text passkey;
    private Label passkeyLabel;
    private Text storekey;
    private Label storekeyLabel;
    private Label aliasLabel;
    private Text alias;
    private PasswordTextFieldDecorator storekeyDec;
    private PasswordTextFieldDecorator passkeyDec;

    public AndroidSigningPropertyPage() {
    }

    protected Control createContents(Composite parent) {
        Composite main = new Composite(parent, SWT.NONE);
        main.setLayout(new GridLayout(1, false));
        
        useProjectSpecific = new Button(main, SWT.CHECK);
        useProjectSpecific.setText("Enable Pr&oject Specific Settings");
        
        useProjectSpecific.setSelection(PropertyUtil.getBoolean(getProject(), PropertyInitializer.ANDROID_PROJECT_SPECIFIC_KEYS));
        useProjectSpecific.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                updateUI();
            }
        });
        
        Label separator = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        signingGroup = new Group(main, SWT.NONE);
        signingGroup.setText("Signing");
        signingGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        keyStore = new FileFieldEditor("dummy.1", "Key&store", signingGroup); //$NON-NLS-1$
        keyStore.setPage(this);

        String keyStorePath = getProject().getProperty(PropertyInitializer.ANDROID_KEYSTORE);
        keyStore.setStringValue(keyStorePath == null ? "" : keyStorePath); //$NON-NLS-1$

        storekeyLabel = new Label(signingGroup, SWT.PASSWORD);
        storekeyLabel.setText("&Keystore password");
        
        storekey = new Text(signingGroup, SWT.BORDER | SWT.SINGLE);
        storekeyDec = new PasswordTextFieldDecorator(storekey);
        GridData storekeyData = new GridData(GridData.FILL_HORIZONTAL);
        storekeyData.horizontalSpan = 2;
        storekey.setLayoutData(storekeyData);
        String storekeyValue = getProject().getProperty(PropertyInitializer.ANDROID_PASS_STORE);
        storekey.setText(storekeyValue == null ? "" : storekeyValue); //$NON-NLS-1$
        
        aliasLabel = new Label(signingGroup, SWT.NONE);
        aliasLabel.setText("Key &Alias");
        
        alias = new Text(signingGroup, SWT.BORDER | SWT.SINGLE);
        GridData aliasData = new GridData(GridData.FILL_HORIZONTAL);
        aliasData.horizontalSpan = 2;
        alias.setLayoutData(aliasData);
        String aliasValue = getProject().getProperty(PropertyInitializer.ANDROID_ALIAS);
        alias.setText(aliasValue == null ? "" : aliasValue);
        
        passkeyLabel = new Label(signingGroup, SWT.NONE);
        passkeyLabel.setText("&Private key password");
        
        passkey = new Text(signingGroup, SWT.BORDER | SWT.SINGLE | SWT.PASSWORD);
        passkeyDec = new PasswordTextFieldDecorator(passkey);
        GridData passkeyData = new GridData(GridData.FILL_HORIZONTAL);
        passkeyData.horizontalSpan = 2;
        passkey.setLayoutData(passkeyData);
        String passkeyValue = getProject().getProperty(PropertyInitializer.ANDROID_PASS_KEY);
        passkey.setText(passkeyValue == null ? "" : passkeyValue);
        
        updateUI();
        
        return main;
    }
    
    private void updateUI() {
        boolean isEnabled = useProjectSpecific.getSelection();
        keyStore.setEnabled(isEnabled, signingGroup);
        storekeyLabel.setEnabled(isEnabled);
        storekeyDec.setEnabled(isEnabled);
        aliasLabel.setEnabled(isEnabled);
        alias.setEnabled(isEnabled);
        passkeyLabel.setEnabled(isEnabled);
        passkeyDec.setEnabled(isEnabled);
        
        if (!useProjectSpecific.getSelection()) {
            keyStore.setStringValue(getProject().getDefaultProperty(PropertyInitializer.ANDROID_KEYSTORE));
            passkey.setText(getProject().getDefaultProperty(PropertyInitializer.ANDROID_PASS_KEY));
            alias.setText(getProject().getDefaultProperty(PropertyInitializer.ANDROID_ALIAS));
            storekey.setText(getProject().getDefaultProperty(PropertyInitializer.ANDROID_PASS_STORE));
        }
    }
    
    public boolean performOk() {
        PropertyUtil.setBoolean(getProject(), PropertyInitializer.ANDROID_PROJECT_SPECIFIC_KEYS, useProjectSpecific.getSelection());
        getProject().setProperty(PropertyInitializer.ANDROID_KEYSTORE, useProjectSpecific.getSelection() ? keyStore.getStringValue() : null);
        getProject().setProperty(PropertyInitializer.ANDROID_PASS_KEY, useProjectSpecific.getSelection() ? passkey.getText() : null);
        getProject().setProperty(PropertyInitializer.ANDROID_PASS_STORE, useProjectSpecific.getSelection() ? storekey.getText() : null);
        
        return true;
    }
    
    private MoSyncProject getProject() {
        IProject wrappedProject = (IProject) getElement();
        MoSyncProject project = MoSyncProject.create(wrappedProject);

        return project;
    }

    
}
