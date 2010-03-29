package com.mobilesorcery.sdk.builder.s60.ui.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.builder.s60.Activator;
import com.mobilesorcery.sdk.builder.s60.PropertyInitializer;
import com.mobilesorcery.sdk.builder.s60.ui.properties.Messages;
import com.mobilesorcery.sdk.ui.PasswordTextFieldDecorator;

public class SymbianSigningPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public SymbianSigningPreferencePage() {
        super(GRID);
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
    }
    
    protected void createFieldEditors() {
        FileFieldEditor keyFile = new FileFieldEditor(PropertyInitializer.S60_KEY_FILE, Messages.SymbianSigningPropertyPage_KeyFile, getFieldEditorParent());
        addField(keyFile);
        FileFieldEditor certFile = new FileFieldEditor(PropertyInitializer.S60_CERT_FILE, Messages.SymbianSigningPropertyPage_CertificateFile, getFieldEditorParent());
        addField(certFile);
        StringFieldEditor passKey = new StringFieldEditor(PropertyInitializer.S60_PASS_KEY, Messages.SymbianSigningPropertyPage_Passkey, getFieldEditorParent());
        addField(passKey);
        PasswordTextFieldDecorator passkeyDec = new PasswordTextFieldDecorator(passKey.getTextControl(getFieldEditorParent()));
    }

    public void init(IWorkbench workbench) {
    }

}
