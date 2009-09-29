package com.mobilesorcery.sdk.wizards.internal;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewFileResourceWizard;

public class NewMoSyncResourceFileWizard extends Wizard implements INewWizard {

    private NewMoSyncResourceFilePage mainPage;
    private IWorkbench workbench;

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.workbench = workbench;
        setWindowTitle("New Resource Wizard");
        mainPage = new NewMoSyncResourceFilePage("New Resource File", selection);
    }

    public void addPages() {
        addPage(mainPage);
    }
    
    public boolean performFinish() {
        IFile file = mainPage.createNewFile();
        try {
            IDE.openEditor(workbench.getActiveWorkbenchWindow().getActivePage(), file);
        } catch (PartInitException e) {
            return false;
        }
        BasicNewFileResourceWizard.selectAndReveal(file, workbench.getActiveWorkbenchWindow());
        return file != null;
    }

}
