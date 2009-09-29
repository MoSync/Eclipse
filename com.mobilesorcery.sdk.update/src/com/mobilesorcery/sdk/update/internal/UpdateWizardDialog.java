package com.mobilesorcery.sdk.update.internal;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class UpdateWizardDialog extends WizardDialog {

	public UpdateWizardDialog(Shell parentShell, IWizard newWizard) {
		super(parentShell, newWizard);
		setHelpAvailable(false);
	}

	public Control createButtonBar(Composite parent) {
		Control mainBar = super.createButtonBar(parent);
		getButton(IDialogConstants.FINISH_ID).setText(Messages.UpdateWizardDialog_UpdateButton);

		return mainBar;
	}

}