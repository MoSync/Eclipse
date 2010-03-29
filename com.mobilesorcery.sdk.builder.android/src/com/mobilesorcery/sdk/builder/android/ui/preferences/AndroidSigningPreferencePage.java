package com.mobilesorcery.sdk.builder.android.ui.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.builder.android.Activator;
import com.mobilesorcery.sdk.builder.android.PropertyInitializer;
import com.mobilesorcery.sdk.ui.PasswordTextFieldDecorator;

public class AndroidSigningPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public AndroidSigningPreferencePage() {
        super(GRID);
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
    }
    
    protected void createFieldEditors() {
        FileFieldEditor keyStore = new FileFieldEditor(PropertyInitializer.ANDROID_KEYSTORE, "Keystore", getFieldEditorParent());
        addField(keyStore);
        StringFieldEditor storepass = new StringFieldEditor(PropertyInitializer.ANDROID_PASS_STORE, "Keystore password", getFieldEditorParent());
        addField(storepass);
        PasswordTextFieldDecorator storepassDec = new PasswordTextFieldDecorator(storepass.getTextControl(getFieldEditorParent()));
        StringFieldEditor alias = new StringFieldEditor(PropertyInitializer.ANDROID_ALIAS, "Alias", getFieldEditorParent());
        addField(alias);
        StringFieldEditor keypass = new StringFieldEditor(PropertyInitializer.ANDROID_PASS_KEY, "Private key password", getFieldEditorParent());
        addField(keypass);
        PasswordTextFieldDecorator keypassDec = new PasswordTextFieldDecorator(keypass.getTextControl(getFieldEditorParent()));
    }

    public void init(IWorkbench workbench) {
    }

}
