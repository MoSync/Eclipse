package com.mobilesorcery.sdk.builder.java.ui.preferences;

import java.util.ArrayList;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.builder.java.Activator;
import com.mobilesorcery.sdk.builder.java.KeystoreCertificateInfo;
import com.mobilesorcery.sdk.builder.java.PropertyInitializer;
import com.mobilesorcery.sdk.builder.java.ui.KeystoreCertificateInfoEditor;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.ui.UpdateListener;
import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;

public class JavaMESigningPreferencePage extends PreferencePage implements IWorkbenchPreferencePage, IUpdatableControl {

    private KeystoreCertificateInfoEditor editor;
    private Button doSign;

    public JavaMESigningPreferencePage() {
        super();
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
    }

    public boolean performOk() {
        ArrayList<KeystoreCertificateInfo> infos = new ArrayList<KeystoreCertificateInfo>();
        if (doSign.getSelection()) {
            infos.add(editor.getKeystoreCertInfo());
        }
        
        getPreferenceStore().setValue(PropertyInitializer.JAVAME_KEYSTORE_CERT_INFOS, KeystoreCertificateInfo.unparse(infos));
        return true;
    }
    
    public void init(IWorkbench workbench) {
    }

    protected Control createContents(Composite parent) {
        // We only support one certificate at this time (in the UI).
        doSign = new Button(parent, SWT.CHECK);
        doSign.addListener(SWT.Selection, new UpdateListener(this));
        doSign.setText("&Sign application for JavaME platforms");
        editor = new KeystoreCertificateInfoEditor(parent, SWT.NONE);
        String keystoreCertInfoStr = getPreferenceStore().getString(PropertyInitializer.JAVAME_KEYSTORE_CERT_INFOS);
        try {
            KeystoreCertificateInfo info = KeystoreCertificateInfo.parseOne(keystoreCertInfoStr);
            editor.setKeystoreCertInfo(info);
            doSign.setSelection(info != null);
        } catch (IllegalArgumentException e) {
            // Ignore.
        }
        
        updateUI();
        return editor;
    }

    public void updateUI() {
        editor.setEnabled(doSign.getSelection());
    }


}
