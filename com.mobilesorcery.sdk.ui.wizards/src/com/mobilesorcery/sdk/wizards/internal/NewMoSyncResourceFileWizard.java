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

    @Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.workbench = workbench;
        setWindowTitle("New Resource Wizard");
        mainPage = new NewMoSyncResourceFilePage("New Resource File", selection);
    }

    @Override
	public void addPages() {
        addPage(mainPage);
    }

    @Override
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
