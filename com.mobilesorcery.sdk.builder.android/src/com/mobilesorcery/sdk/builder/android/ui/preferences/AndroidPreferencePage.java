package com.mobilesorcery.sdk.builder.android.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.builder.android.PropertyInitializer;

public class AndroidPreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	@Override
	public void init(IWorkbench workbench) {
		
	}

	@Override
	protected void createFieldEditors() {
		BooleanFieldEditor adbDebugEditor = new BooleanFieldEditor(PropertyInitializer.ADB_DEBUG_LOG, "Dump ADB debug logs when executing Android apps", getFieldEditorParent());
		addField(adbDebugEditor);
	}

	
}
