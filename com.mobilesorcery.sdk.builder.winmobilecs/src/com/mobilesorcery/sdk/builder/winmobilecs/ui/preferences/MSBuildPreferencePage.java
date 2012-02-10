package com.mobilesorcery.sdk.builder.winmobilecs.ui.preferences;

import java.io.File;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.builder.winmobilecs.MSBuild;
import com.mobilesorcery.sdk.builder.winmobilecs.WinMobileCSPlugin;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.Note;
import com.mobilesorcery.sdk.ui.UIUtils;
import com.mobilesorcery.sdk.ui.UpdateListener;
import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;

public class MSBuildPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage, IUpdatableControl {

	private Button onlyGenerateEditor;
	private DirectoryFieldEditor msBuildPathEditor;
	private Label infoLabel;
	private Composite main;
	private Link guessLocationLink;

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
		onlyGenerateEditor
				.setText("Only generate Visual Studio project for Windows Phone devices (do not build it)");
		onlyGenerateEditor.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT,
				true, false, 3, 1));
		onlyGenerateEditor.addListener(SWT.Selection, new UpdateListener(this));
		onlyGenerateEditor.setSelection(!getPreferenceStore().getBoolean(
				WinMobileCSPlugin.BUILD_WITH_VS));

		boolean isWin = Util.isWindows();
		if (isWin) {
			msBuildPathEditor = new DirectoryFieldEditor(
					WinMobileCSPlugin.MS_BUILD_PATH,
					"&Visual Studio location:", main) {
				@Override
				protected String changePressed() {
					String dir = super.changePressed();
					// We add the MSBuild.exe by default...
					return dir + File.separator + "MSBuild.exe";
				}
			};
			msBuildPathEditor.setPreferenceStore(getPreferenceStore());
			msBuildPathEditor.setFilterPath(new File(WinMobileCSPlugin
					.getSystemRoot(), "/Microsoft.NET/Framework"));
			msBuildPathEditor.setEmptyStringAllowed(true);
			msBuildPathEditor.load();

			infoLabel = new Label(main, SWT.WRAP);
			infoLabel.setFont(MosyncUIPlugin.getDefault().getFont(
					MosyncUIPlugin.FONT_INFO_TEXT));
			infoLabel
					.setText("Select which Visual Studio SDKs to use for building. To let the IDE try to find it, click the \"Guess Location\" link.\n(If you select using the 'Browse...' button, select the directory that contains the MSBuild.exe executable, usually located at something like C:\\Windows\\Microsoft.NET\\Framework\\{VERSION}.)");
			infoLabel.setLayoutData(new GridData(SWT.FILL,
					SWT.DEFAULT, true, false, 2, 1));

			guessLocationLink = new Link(main, SWT.NONE);
			guessLocationLink.setText("<a>Guess Location</a>");
			guessLocationLink.setLayoutData(new GridData(SWT.RIGHT,
					SWT.DEFAULT, true, false));
			guessLocationLink.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					MSBuild guessedMSBuild = MSBuild.guess();
					if (guessedMSBuild == null) {
						MessageDialog
								.openWarning(
										getShell(),
										"Could not find Visual Studio/MSBuild.exe",
										"Could not guess location.\nDid you install Visual Studio for Windows Phone?");
					} else {
						msBuildPathEditor.setStringValue(guessedMSBuild
								.getToolPath().getAbsolutePath());
					}
				}
			});
		} else {
			Note info = new Note(main, SWT.NONE);
			info.setText("These preferences are only available on Windows");
			onlyGenerateEditor.setEnabled(false);
			info.moveAbove(null);
			info.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false,
					3, 1));
		}

		updateUI();

		return main;
	}

	@Override
	public boolean performOk() {
		getPreferenceStore().setValue(WinMobileCSPlugin.BUILD_WITH_VS,
				!onlyGenerateEditor.getSelection());
		if (msBuildPathEditor != null) {
			msBuildPathEditor.store();
		}
		return true;
	}

	@Override
	public void performDefaults() {
		onlyGenerateEditor.setSelection(!getPreferenceStore()
				.getDefaultBoolean(WinMobileCSPlugin.BUILD_WITH_VS));
		if (msBuildPathEditor != null) {
			msBuildPathEditor.loadDefault();
		}
	}

	@Override
	public void updateUI() {
		boolean enabled = !onlyGenerateEditor.getSelection();
		if (msBuildPathEditor != null) {
			msBuildPathEditor.setEnabled(enabled, main);
		}
		if (guessLocationLink != null) {
			guessLocationLink.setEnabled(enabled);
		}
	}
}
