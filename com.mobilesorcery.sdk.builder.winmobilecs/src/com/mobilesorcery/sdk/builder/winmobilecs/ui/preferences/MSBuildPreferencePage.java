package com.mobilesorcery.sdk.builder.winmobilecs.ui.preferences;


import java.io.File;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.builder.winmobilecs.WinMobileCSPlugin;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.Note;
import com.mobilesorcery.sdk.ui.UIUtils;
import com.mobilesorcery.sdk.ui.UpdateListener;
import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;

public class MSBuildPreferencePage extends PreferencePage implements IWorkbenchPreferencePage, IUpdatableControl {

	private Button onlyGenerateEditor;
	private FileFieldEditor msBuildPathEditor;
	private Label infoLabel;
	private Composite main;

	public MSBuildPreferencePage() {
		setPreferenceStore(WinMobileCSPlugin.getDefault().getPreferenceStore());
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected Control createContents(Composite parent) {
		main = new Composite(parent, SWT.NONE);
		main.setLayout(UIUtils.newPrefsLayout(3));
		onlyGenerateEditor = new Button(main, SWT.CHECK);
		onlyGenerateEditor.setText("Only generate Visual Studio project for Windows Phone devices (do not build it)");
		onlyGenerateEditor.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false, 3, 1));
		onlyGenerateEditor.addListener(SWT.Selection, new UpdateListener(this));
		onlyGenerateEditor.setSelection(!getPreferenceStore().getBoolean(WinMobileCSPlugin.BUILD_WITH_VS));

		msBuildPathEditor = new FileFieldEditor(WinMobileCSPlugin.MS_BUILD_PATH, "&Visual Studio location:", main);
		msBuildPathEditor.setPreferenceStore(getPreferenceStore());
		msBuildPathEditor.setFilterPath(new File(WinMobileCSPlugin.getSystemRoot(), "/Microsoft.NET/Framework"));
		msBuildPathEditor.setEmptyStringAllowed(true);
		msBuildPathEditor.load();

	    infoLabel = new Label(main, SWT.WRAP);
        infoLabel.setFont(MosyncUIPlugin.getDefault().getFont(MosyncUIPlugin.FONT_INFO_TEXT));
        infoLabel.setText("Select which Visual Studio SDKs to use for building.\n(Select the msbuild.exe executable.)");
        infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false, 3, 1));

		boolean isWin = Util.isWindows();
		if (!isWin) {
			Note info = new Note(main, SWT.NONE);
			info.setText("These preferences are only available on Windows");
			onlyGenerateEditor.setEnabled(false);
			msBuildPathEditor.setEnabled(false, main);
			infoLabel.setEnabled(false);
			info.moveAbove(null);
			info.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false, 3, 1));
		}

		updateUI();

		return main;
	}


	@Override
	public boolean performOk() {
		getPreferenceStore().setValue(WinMobileCSPlugin.BUILD_WITH_VS, !onlyGenerateEditor.getSelection());
		msBuildPathEditor.store();
		return true;
	}

	@Override
	public void performDefaults() {
		onlyGenerateEditor.setSelection(!getPreferenceStore().getDefaultBoolean(WinMobileCSPlugin.BUILD_WITH_VS));
		msBuildPathEditor.loadDefault();
	}

	@Override
	public void updateUI() {
		msBuildPathEditor.setEnabled(Util.isWindows() && !onlyGenerateEditor.getSelection(), main);
		infoLabel.setVisible(Util.isWindows() && !onlyGenerateEditor.getSelection());
	}
}
