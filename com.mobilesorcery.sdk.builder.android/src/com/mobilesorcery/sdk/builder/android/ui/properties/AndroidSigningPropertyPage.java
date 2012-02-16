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

import org.eclipse.jface.dialogs.IMessageProvider;
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
import org.eclipse.ui.IWorkbenchPropertyPage;

import com.mobilesorcery.sdk.builder.android.Activator;
import com.mobilesorcery.sdk.builder.android.PropertyInitializer;
import com.mobilesorcery.sdk.builder.java.KeystoreCertificateInfo;
import com.mobilesorcery.sdk.builder.java.ui.KeystoreCertificateInfoEditor;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.PreferenceStorePropertyOwner;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.SecurePropertyException;
import com.mobilesorcery.sdk.ui.DefaultMessageProvider;
import com.mobilesorcery.sdk.ui.MoSyncPropertyPage;
import com.mobilesorcery.sdk.ui.UpdateListener;

public class AndroidSigningPropertyPage extends MoSyncPropertyPage implements IWorkbenchPropertyPage {

    private Button useProjectSpecific;
    private Group signingGroup;
    private KeystoreCertificateInfoEditor keyCertUI;
	private KeystoreCertificateInfo globalCertInfo;
	private KeystoreCertificateInfo projectCertInfo;
	private KeystoreCertificateInfo currentCertInfo;
	private Boolean wasEnabled;

    public AndroidSigningPropertyPage() {
    	super(true);
    }

    @Override
	protected Control createContents(Composite parent) {
        Composite main = new Composite(parent, SWT.NONE);
        main.setLayout(new GridLayout(1, false));

        useProjectSpecific = new Button(main, SWT.CHECK);
        useProjectSpecific.setText("Enable Pr&oject Specific Settings");

        useProjectSpecific.setSelection(PropertyUtil.getBoolean(getProject(), PropertyInitializer.ANDROID_PROJECT_SPECIFIC_KEYS));
        useProjectSpecific.addListener(SWT.Selection, new UpdateListener(this));

        Label separator = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        signingGroup = new Group(main, SWT.NONE);
        signingGroup.setText("Signing");
        signingGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        signingGroup.setLayout(new GridLayout(1, false));

        keyCertUI = new KeystoreCertificateInfoEditor(signingGroup, SWT.NONE);
        keyCertUI.setUpdatable(this);
        keyCertUI.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        init();
        updateUI();

        return main;
    }

    private void init() {
    	globalCertInfo = KeystoreCertificateInfo.loadOne(PropertyInitializer.ANDROID_KEYSTORE_CERT_INFO,
        		new PreferenceStorePropertyOwner(Activator.getDefault().getPreferenceStore()),
        	    CoreMoSyncPlugin.getDefault().getSecureProperties());
    	projectCertInfo = KeystoreCertificateInfo.loadOne(PropertyInitializer.ANDROID_KEYSTORE_CERT_INFO, getProject(), getProject().getSecurePropertyOwner());
    	currentCertInfo = globalCertInfo;
    }

    @Override
	public void updateUI() {
    	 boolean isEnabled = useProjectSpecific.getSelection();
         keyCertUI.setEnabled(isEnabled);
         boolean changedState = wasEnabled == null || isEnabled != wasEnabled;
         if (changedState) {
         	wasEnabled = isEnabled;
 	        if (isEnabled) {
 	        	keyCertUI.setKeystoreCertInfo(projectCertInfo);
 	        } else {
 	        	projectCertInfo = currentCertInfo;
 	        	keyCertUI.setKeystoreCertInfo(globalCertInfo);
 	        }
         }
         currentCertInfo = keyCertUI.getKeystoreCertInfo();
         setMessage(currentCertInfo.validate(true));
    }

    @Override
	public boolean performOk() {
        PropertyUtil.setBoolean(getProject(), PropertyInitializer.ANDROID_PROJECT_SPECIFIC_KEYS, useProjectSpecific.getSelection());
        try {
			keyCertUI.getKeystoreCertInfo().store(PropertyInitializer.ANDROID_KEYSTORE_CERT_INFO, getProject(), getProject().getSecurePropertyOwner());
		} catch (SecurePropertyException e) {
			handleSecurePropertyException(e);
			return false;
		}
        return true;
    }

    @Override
	public void performDefaults() {
    	keyCertUI.setToDefault();
    	useProjectSpecific.setSelection(PropertyUtil.toBoolean(getProject().getDefaultProperty(PropertyInitializer.ANDROID_PROJECT_SPECIFIC_KEYS)));
    	updateUI();
    }

    private void handleSecurePropertyException(SecurePropertyException e) {
		setMessage(new DefaultMessageProvider(e.getMessage(), IMessageProvider.WARNING));
	}

}
