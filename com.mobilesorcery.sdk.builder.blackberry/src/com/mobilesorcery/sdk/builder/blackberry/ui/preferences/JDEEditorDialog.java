package com.mobilesorcery.sdk.builder.blackberry.ui.preferences;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.mobilesorcery.sdk.builder.blackberry.GuessVersionJob;
import com.mobilesorcery.sdk.builder.blackberry.JDE;
import com.mobilesorcery.sdk.builder.blackberry.Simulator;
import com.mobilesorcery.sdk.core.Version;
import com.mobilesorcery.sdk.ui.UIUtils;

public class JDEEditorDialog extends Dialog {

	private JDE jde;
	private boolean showVersionField;
	private StringButtonFieldEditor versionEditor;
	private DirectoryFieldEditor location;
	private int toolType;

	protected JDEEditorDialog(Shell parentShell) {
		super(parentShell);
	}

	public void setShowVersionField(boolean showVersionField) {
		this.showVersionField = showVersionField;
	}

	public void setInitial(JDE jde) {
		this.jde = jde;
	}

	public void setToolType(int toolType) {
		this.toolType = toolType;
	}

	public JDE getEdited() {
		return jde;
	}

	@Override
	public Control createDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		Label instructions = new Label(main, SWT.NONE);
		String toolStr = toolType == JDE.TYPE_DEV_TOOLS ? "JDE" : "Simulator";
		instructions.setText(MessageFormat.format("Select location of BlackBerry {0}", toolStr));
		// We just want the UI, we don't care about the pref store
		location = new DirectoryFieldEditor("", MessageFormat.format("{0} Path:", toolStr), main);
		location.getTextControl(main).setLayoutData(new GridData(UIUtils.getDefaultFieldSize(), SWT.DEFAULT));
		IPath path = jde.getLocation();
		location.setStringValue(path == null ? "" : path.toOSString());
		instructions.setLayoutData(new GridData(GridData.FILL, SWT.DEFAULT, true, false, location.getNumberOfControls(), 1));

		if (showVersionField) {
			versionEditor = new StringButtonFieldEditor("", "JDE version:", main) {
				@Override
				protected String changePressed() {
					GuessVersionJob job = new GuessVersionJob();
					job.setJDE(createJDEFromUI());
					job.schedule();
					try {
						job.join();
					} catch (InterruptedException e) {
						// Cancelled.
					}
					Version version = job.getVersion();
					return version == null ? "" : version.asCanonicalString();
				}
			};
			versionEditor.setChangeButtonText("Compute version");
			Version version = jde.getVersion();
			versionEditor.setStringValue(version == null ? "" : version.asCanonicalString());
		}

		main.setLayout(new GridLayout(location.getNumberOfControls(), false));
		return main;
	}

	@Override
	public void okPressed() {
		jde = createJDEFromUI();
		super.okPressed();
	}

	private JDE createJDEFromUI() {
		Version version = versionEditor == null ? null : new Version(versionEditor.getStringValue());
		return JDE.create(toolType, new Path(location.getStringValue()), version);
	}
}
