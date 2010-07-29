package com.mobilesorcery.sdk.builder.java.ui.properties;

import java.util.ArrayList;

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

import com.mobilesorcery.sdk.builder.java.KeystoreCertificateInfo;
import com.mobilesorcery.sdk.builder.java.PropertyInitializer;
import com.mobilesorcery.sdk.builder.java.ui.KeystoreCertificateInfoEditor;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.ui.MoSyncPropertyPage;
import com.mobilesorcery.sdk.ui.UpdateListener;

public class JavaMESigningPropertyPage extends MoSyncPropertyPage {

    private Button useProjectSpecific;
    private KeystoreCertificateInfoEditor keyCertUI;
    private Button doSign;

    protected Control createContents(Composite parent) {
        Composite main = new Composite(parent, SWT.NONE);
        main.setLayout(new GridLayout(1, false));
        
        useProjectSpecific = new Button(main, SWT.CHECK);
        useProjectSpecific.setText("Enable Pr&oject Specific Settings");
        
        useProjectSpecific.setSelection(PropertyUtil.getBoolean(getProject(), PropertyInitializer.JAVAME_PROJECT_SPECIFIC_KEYS));
        UpdateListener listener = new UpdateListener(this);
        useProjectSpecific.addListener(SWT.Selection, listener);
        
        Label separator = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        Group signingGroup = new Group(main, SWT.NONE);
        signingGroup.setText("Signing");
        signingGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        signingGroup.setLayout(new GridLayout(1, false));
        
        doSign = new Button(signingGroup, SWT.CHECK);
        doSign.setText("&Sign application for JavaME platforms");
        doSign.addListener(SWT.Selection, listener);
        
        keyCertUI = new KeystoreCertificateInfoEditor(signingGroup, SWT.NONE);
        keyCertUI.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        try {
            KeystoreCertificateInfo keystoreCertInfo = KeystoreCertificateInfo.parseOne(getProject().getProperty(PropertyInitializer.JAVAME_KEYSTORE_CERT_INFOS));
            keyCertUI.setKeystoreCertInfo(keystoreCertInfo);
            doSign.setSelection(keystoreCertInfo != null);
        } catch (IllegalArgumentException e) {
            // Ignore.
        }
        
        updateUI();
        return main;
    }
    
    public void updateUI() {
        keyCertUI.setEnabled(useProjectSpecific.getSelection() && doSign.getSelection());
        doSign.setEnabled(useProjectSpecific.getSelection());
        
        if (!useProjectSpecific.getSelection()) {
            KeystoreCertificateInfo defaultInfo = KeystoreCertificateInfo.parseOne(getProject().getDefaultProperty(PropertyInitializer.JAVAME_KEYSTORE_CERT_INFOS));
            keyCertUI.setKeystoreCertInfo(defaultInfo);
            doSign.setSelection(defaultInfo != null);
        } 
    }
    
    public boolean performOk() {
        PropertyUtil.setBoolean(getProject(), PropertyInitializer.JAVAME_PROJECT_SPECIFIC_KEYS, useProjectSpecific.getSelection());
        ArrayList<KeystoreCertificateInfo> infos = new ArrayList<KeystoreCertificateInfo>();
        if (doSign.getSelection()) {
            infos.add(keyCertUI.getKeystoreCertInfo());
        }
        
        getProject().setProperty(PropertyInitializer.JAVAME_KEYSTORE_CERT_INFOS, useProjectSpecific.getSelection() ? KeystoreCertificateInfo.unparse(infos) : null);
        return super.performOk();
    }
}
