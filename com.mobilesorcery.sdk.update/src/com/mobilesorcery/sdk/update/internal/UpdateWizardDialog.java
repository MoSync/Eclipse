/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
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