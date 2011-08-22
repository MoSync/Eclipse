package com.mobilesorcery.sdk.builder.android.ui.preferences;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.builder.android.Activator;
import com.mobilesorcery.sdk.builder.android.launch.ADB;

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
		DirectoryFieldEditor sdkLocation = new DirectoryFieldEditor(Activator.EXTERNAL_SDK_PATH, "&SDK Location", getFieldEditorParent()) {
			@Override
			protected boolean checkState() {
				ADB adb = ADB.findADB(new Path(getStringValue()));
				if (!adb.isValid()) {
					setMessage("This is an invalid Android SDK location (found no ADB binary)", IMessageProvider.WARNING);
				} else {
					setMessage(null, IMessageProvider.NONE);
				}
				return adb.isValid() && super.checkState();
			}
		};
		sdkLocation.setValidateStrategy(DirectoryFieldEditor.VALIDATE_ON_KEY_STROKE);
		addField(sdkLocation);
	}

}
