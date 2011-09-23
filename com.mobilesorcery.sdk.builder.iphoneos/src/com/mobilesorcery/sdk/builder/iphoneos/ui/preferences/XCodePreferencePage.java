package com.mobilesorcery.sdk.builder.iphoneos.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.builder.iphoneos.Activator;

public class XCodePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public XCodePreferencePage() {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected void createFieldEditors() {
		BooleanFieldEditor editor = new BooleanFieldEditor(Activator.ONLY_GENERATE_XCODE_PROJECT, "Only generate Xcode project for iOS devices (do not build it)", getFieldEditorParent());
		addField(editor);
	}

}