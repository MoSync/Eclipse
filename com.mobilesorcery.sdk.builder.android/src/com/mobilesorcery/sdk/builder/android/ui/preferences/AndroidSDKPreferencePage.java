package com.mobilesorcery.sdk.builder.android.ui.preferences;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.builder.android.Activator;

public class AndroidSDKPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage{

    public AndroidSDKPreferencePage() {
        super();
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
    }

	@Override
	public void init(IWorkbench workbench) {
		
	}

	@Override
	protected void createFieldEditors() {
		DirectoryFieldEditor sdkLocation = new DirectoryFieldEditor(Activator.EXTERNAL_SDK_PATH, "&SDK Location", getFieldEditorParent());
		addField(sdkLocation);
	}

}
