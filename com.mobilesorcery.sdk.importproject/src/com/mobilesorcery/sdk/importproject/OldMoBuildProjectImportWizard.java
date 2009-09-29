package com.mobilesorcery.sdk.importproject;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.util.StatusHandler;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.statushandlers.StatusManager;

public class OldMoBuildProjectImportWizard extends Wizard implements IImportWizard, INewWizard {

    private WizardOldMobuildProjectsImportPage mainPage;

    public OldMoBuildProjectImportWizard() {
        super();
    }

    public boolean performFinish() {
        File[] projectDescriptionFiles = mainPage.getProjectDescriptionFiles();
        if (projectDescriptionFiles == null) {
            return true;
        }
        
        ImportProjectsRunnable importProjectsRunnable = new ImportProjectsRunnable(projectDescriptionFiles, mainPage.getCopyStrategy());
        
        try {
            getContainer().run(true, true, importProjectsRunnable);
        } catch (Exception e) {
            e.printStackTrace();
            StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e), StatusManager.SHOW);
        } 
        
        return true;
    }

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setNeedsProgressMonitor(true);
        
        mainPage = new WizardOldMobuildProjectsImportPage();
        addPage(mainPage);
    }

}
