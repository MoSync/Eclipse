package com.mobilesorcery.sdk.builder.android.ui.preferences;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.builder.android.Activator;
import com.mobilesorcery.sdk.builder.android.launch.ADB;

public class AndroidSDKPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage{

    private final class SDKFieldEditor extends DirectoryFieldEditor {
		private SDKFieldEditor(String name, String labelText, Composite parent) {
			super(name, labelText, parent);
			setValidateStrategy(VALIDATE_ON_KEY_STROKE);
		}

		@Override
		protected boolean checkState() {
			ADB adb = ADB.findADB(new Path(getStringValue()));
			clearErrorMessage();
			if (!adb.isValid()) {
				setMessage("This is an invalid Android SDK location (found no ADB binary)", IMessageProvider.WARNING);
			} else {
				setMessage(null, IMessageProvider.NONE);
			}
			return adb.isValid() && super.checkState();
		}
	}

	public AndroidSDKPreferencePage() {
        super();
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
    }

	@Override
	public void init(IWorkbench workbench) {

	}

	@Override
	protected void createFieldEditors() {
		SDKFieldEditor sdkLocation = new SDKFieldEditor(Activator.EXTERNAL_SDK_PATH, "&SDK Location", getFieldEditorParent());
		addField(sdkLocation);
	}

}
