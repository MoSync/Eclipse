package com.mobilesorcery.sdk.fontsupport.internal.wizard;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class GenerateMOFWizardAction implements IObjectActionDelegate {

	private IFile file;
	private IWorkbenchPart targetPart;

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
	}

	public void run(IAction action) {
		if (file != null) {
			File osFile = file.getLocation().toFile();

			GenerateMOFWizard wizard = new GenerateMOFWizard();
			wizard.setFilename(osFile);
			Shell shell = targetPart.getSite().getShell();
			WizardDialog dialog = new WizardDialog(shell, wizard);
			dialog.open();
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		file = null;
		if (selection instanceof IStructuredSelection) {
			Object first = ((IStructuredSelection) selection).getFirstElement();
			if (first instanceof IAdaptable) {
				first = ((IAdaptable) first).getAdapter(IResource.class);
			}

			if (first instanceof IFile) {
				file = (IFile) first;
			}
		}
	}

}
