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
import com.mobilesorcery.sdk.builder.java.KeystoreCertificateInfo;
import com.mobilesorcery.sdk.builder.java.ui.KeystoreCertificateInfoEditor;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.ui.PasswordTextFieldDecorator;

public class AndroidSigningPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {

    private Button useProjectSpecific;
    private Group signingGroup;
    private KeystoreCertificateInfoEditor keyCertUI;

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
        signingGroup.setLayout(new GridLayout(1, false));
        
        keyCertUI = new KeystoreCertificateInfoEditor(signingGroup, SWT.NONE);
        keyCertUI.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        updateUI();
        
        return main;
    }
    
    private void updateUI() {
        boolean isEnabled = useProjectSpecific.getSelection();
        keyCertUI.setEnabled(isEnabled);
        if (!useProjectSpecific.getSelection()) {
            KeystoreCertificateInfo defaultInfo = KeystoreCertificateInfo.parseOne(getProject().getDefaultProperty(PropertyInitializer.ANDROID_KEYSTORE_CERT_INFO));
            keyCertUI.setKeystoreCertInfo(defaultInfo);
        }
    }
    
    public boolean performOk() {
        PropertyUtil.setBoolean(getProject(), PropertyInitializer.ANDROID_PROJECT_SPECIFIC_KEYS, useProjectSpecific.getSelection());
        getProject().setProperty(PropertyInitializer.ANDROID_KEYSTORE_CERT_INFO, useProjectSpecific.getSelection() ? KeystoreCertificateInfo.unparse(keyCertUI.getKeystoreCertInfo()) : null);
        return true;
    }
    
    private MoSyncProject getProject() {
        IProject wrappedProject = (IProject) getElement();
        MoSyncProject project = MoSyncProject.create(wrappedProject);

        return project;
    }

    
}
