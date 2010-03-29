package com.mobilesorcery.sdk.builder.android.ui.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.builder.android.Activator;
import com.mobilesorcery.sdk.builder.android.PropertyInitializer;

public class AndroidSigningPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public AndroidSigningPreferencePage() {
        super(GRID);
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
    }
    
    protected void createFieldEditors() {
        FileFieldEditor keyStore = new FileFieldEditor(PropertyInitializer.KEYSTORE, "Keystore", getFieldEditorParent());
        addField(keyStore);
        StringFieldEditor storepass = new StringFieldEditor(PropertyInitializer.ANDROID_PASS_STORE, "Keystore password", getFieldEditorParent());
        addField(storepass);
        StringFieldEditor keypass = new StringFieldEditor(PropertyInitializer.ANDROID_PASS_KEY, "Private key password", getFieldEditorParent());
        addField(keypass);
    }

    public void init(IWorkbench workbench) {
    }

}
