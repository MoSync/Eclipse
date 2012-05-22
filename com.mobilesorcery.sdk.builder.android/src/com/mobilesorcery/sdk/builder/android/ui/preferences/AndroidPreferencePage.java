package com.mobilesorcery.sdk.builder.android.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.builder.android.Activator;
import com.mobilesorcery.sdk.builder.android.PropertyInitializer;

public class AndroidPreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}

	@Override
	protected void createFieldEditors() {
		BooleanFieldEditor adbDebugEditor = new BooleanFieldEditor(PropertyInitializer.ADB_DEBUG_LOG, "Dump logcat in console when executing Android apps", getFieldEditorParent());
		StringFieldEditor logcatFormatEditor = new StringFieldEditor(PropertyInitializer.ADB_LOGCAT_ARGS, "Logcat arguments:", getFieldEditorParent());
		addField(adbDebugEditor);
		addField(logcatFormatEditor);
	}

	
}
