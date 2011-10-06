package com.mobilesorcery.sdk.builder.iphoneos.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.builder.iphoneos.Activator;
import com.mobilesorcery.sdk.ui.UIUtils;

public class XCodePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private BooleanFieldEditor editor;

	public XCodePreferencePage() {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(UIUtils.newPrefsLayout(2));
		editor = new BooleanFieldEditor(Activator.ONLY_GENERATE_XCODE_PROJECT, "Only generate Xcode project for iOS devices (do not build it)", main);
		editor.setPreferenceStore(getPreferenceStore());
		editor.load();
		boolean isMac = Util.isMac();
		if (!isMac) {
			Label info = new Label(main, SWT.NONE);
			info.setText("(This option is only available on Mac OS X)");
			editor.setEnabled(false, main);
		}
		return main;
	}

	@Override
	public boolean performOk() {
		editor.store();
		return true;
	}

	@Override
	public void performDefaults() {
		editor.loadDefault();
	}
}