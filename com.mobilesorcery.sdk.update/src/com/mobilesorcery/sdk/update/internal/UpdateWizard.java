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