package com.mobilesorcery.sdk.extensionsupport.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.mobilesorcery.sdk.extensionsupport.ExtensionSupportBuildStep;
import com.mobilesorcery.sdk.extensionsupport.ExtensionSupportBuildStep.Factory;
import com.mobilesorcery.sdk.ui.BuildStepEditor;

public class ExtensionSupportBuildStepEditor extends BuildStepEditor {

	private Button doAutoInstall;
	private Button doGenerateStubs;
	
	@Override
	public void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Edit Extension Build Step");
	}

	@Override
	public Control createDialogArea(Composite parent) {
		Composite main = (Composite) super.createDialogArea(parent);
		main.setLayoutData(new GridData(GridData.FILL));
		main.setLayout(new GridLayout(2, false));
		if (getFactory().getPhase().equals(ExtensionSupportBuildStep.PACK_PHASE)) {
			doAutoInstall = new Button(main, SWT.CHECK);
			doAutoInstall.setText("&Automatically install extension");
			doAutoInstall.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT, true, false, 2, 1));
			doAutoInstall.setSelection(getFactory().shouldUpdateInstallation());
		} else {
			doGenerateStubs = new Button(main, SWT.CHECK);
			doGenerateStubs.setText("&Always Generate Stubs");
			doGenerateStubs.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT, true, false, 2, 1));
			doGenerateStubs.setSelection(getFactory().shouldGenerateStubs());
		}
		return main;
	}

	@Override
	public void okPressed() {
		if (doAutoInstall != null) {
			getFactory().shouldUpdateInstallation(doAutoInstall.getSelection());
		}
		if (doGenerateStubs != null) {
			getFactory().shouldGenerateStubs(doGenerateStubs.getSelection());
		}
		super.okPressed();
	}

	private ExtensionSupportBuildStep.Factory getFactory() {
		return (Factory) factory;
	}
}
