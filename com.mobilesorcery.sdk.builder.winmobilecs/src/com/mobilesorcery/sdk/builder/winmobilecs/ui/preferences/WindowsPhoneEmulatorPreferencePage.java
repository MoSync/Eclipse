package com.mobilesorcery.sdk.builder.winmobilecs.ui.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.builder.winmobilecs.WinMobileCSPlugin;

public class WindowsPhoneEmulatorPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(WinMobileCSPlugin.getDefault().getPreferenceStore());
	}

	@Override
	protected void createFieldEditors() {
		FileFieldEditor emulatorLocation = new FileFieldEditor(WinMobileCSPlugin.EMULATOR_LOCATION, "Emulator location", getFieldEditorParent());
		emulatorLocation.setPreferenceStore(getPreferenceStore());
		emulatorLocation.setFileExtensions(new String[] { "*.exe", "*.*" });
		addField(emulatorLocation);
	}

}
