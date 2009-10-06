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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;

public class UpdateWizard extends Wizard {

	UpdateAvailablePage uaPage;
	UpdateMessagePage  umPage;

	protected IWorkbench workbench;
	private String updateMessage;

	public UpdateWizard(String updateMessage) {
		super();
		setWindowTitle(Messages.UpdateWizard_WindowTitle);
		this.updateMessage = updateMessage;
	}

	public void addPages() {
		uaPage = new UpdateAvailablePage(""); //$NON-NLS-1$
		addPage(uaPage);
		umPage = new UpdateMessagePage(""); //$NON-NLS-1$
		umPage.setUpdateMessage(updateMessage);
		addPage(umPage);
	}

	public boolean performFinish() {
		boolean result = MessageDialog.openQuestion(getShell(), Messages.UpdateWizard_RestartDialogTitle, Messages.UpdateWizard_RestartDialogMessage);
		return result;
	}

	public boolean canFinish(){
		if (umPage.canFinish()) {
			return true;
		}
		return false;
	}

}