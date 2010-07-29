package com.mobilesorcery.sdk.builder.android.ui.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.builder.android.Activator;
import com.mobilesorcery.sdk.builder.android.PropertyInitializer;
import com.mobilesorcery.sdk.builder.java.KeystoreCertificateInfo;
import com.mobilesorcery.sdk.builder.java.ui.KeystoreCertificateInfoEditor;

public class AndroidSigningPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    private KeystoreCertificateInfoEditor editor;

    public AndroidSigningPreferencePage() {
        super();
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
    }
    
    /*protected void createFieldEditors() {
        keyStore = new FileFieldEditor(PropertyInitializer.ANDROID_KEYSTORE_CERT_INFO, "Keystore", getFieldEditorParent());
        addField(keyStore);
        storepass = new StringFieldEditor(PropertyInitializer.ANDROID_PASS_STORE, "Keystore password", getFieldEditorParent());
        addField(storepass);
        PasswordTextFieldDecorator storepassDec = new PasswordTextFieldDecorator(storepass.getTextControl(getFieldEditorParent()));
        StringFieldEditor alias = new StringFieldEditor(PropertyInitializer.ANDROID_ALIAS, "Alias", getFieldEditorParent());
        addField(alias);
        StringFieldEditor keypass = new StringFieldEditor(PropertyInitializer.ANDROID_PASS_KEY, "Private key password", getFieldEditorParent());
        addField(keypass);
        PasswordTextFieldDecorator keypassDec = new PasswordTextFieldDecorator(keypass.getTextControl(getFieldEditorParent()));
    }*/

    public boolean performOk() {
        KeystoreCertificateInfo info = editor.getKeystoreCertInfo();
        getPreferenceStore().setValue(PropertyInitializer.ANDROID_KEYSTORE_CERT_INFO, KeystoreCertificateInfo.unparse(info));
        return true;
    }
    
    public void init(IWorkbench workbench) {
    }

    protected Control createContents(Composite parent) {
        editor = new KeystoreCertificateInfoEditor(parent, SWT.NONE);
        String keystoreCertInfoStr = getPreferenceStore().getString(PropertyInitializer.ANDROID_KEYSTORE_CERT_INFO);
        try {
            KeystoreCertificateInfo info = KeystoreCertificateInfo.parseOne(keystoreCertInfoStr);
            editor.setKeystoreCertInfo(info);
        } catch (IllegalArgumentException e) {
            // Ignore.
        }
        
        return editor;
    }

}
