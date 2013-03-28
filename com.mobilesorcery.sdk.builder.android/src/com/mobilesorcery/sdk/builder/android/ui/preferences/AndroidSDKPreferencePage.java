package com.mobilesorcery.sdk.builder.android.ui.preferences;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
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
			boolean isEmpty = getStringValue().isEmpty();
			ADB adb = ADB.findADB(new Path(getStringValue()));
			clearErrorMessage();
			if (!adb.isValid()) {
				setMessage("This is an invalid Android SDK location (found no ADB binary)", IMessageProvider.WARNING);
			} else {
				setMessage(null, IMessageProvider.NONE);
			}
			return isEmpty || (adb.isValid() && super.checkState());
		}
	}

	public AndroidSDKPreferencePage() {
        super();
        noDefaultAndApplyButton();
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
    }

	@Override
	public void init(IWorkbench workbench) {

	}

	@Override
	protected void createFieldEditors() {
		SDKFieldEditor sdkLocation = new SDKFieldEditor(Activator.EXTERNAL_SDK_PATH, "&SDK Location", getFieldEditorParent());
		DirectoryFieldEditor ndkLocation = new DirectoryFieldEditor(Activator.NDK_PATH, "&NDK Location", getFieldEditorParent());
		IntegerFieldEditor platformVersion = new IntegerFieldEditor(Activator.NDK_PLATFORM_VERSION, "NDK &Platform Version", getFieldEditorParent());
		addField(sdkLocation);
		addField(ndkLocation);
		addField(platformVersion);
	}

}
