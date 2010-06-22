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
package com.mobilesorcery.sdk.importproject;

import java.io.File;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.statushandlers.StatusManager;

import com.mobilesorcery.sdk.ui.ImportProjectsRunnable;

/**
 * 
 * @author Mattias Bybro
 * @deprecated Use MoSyncExternalProjectImportWizard instead - .mopro files are no longer used.
 * This file is kept until we've migrated those pieces of functionality that we'll need
 * for the non-.mopro solution
 */
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
