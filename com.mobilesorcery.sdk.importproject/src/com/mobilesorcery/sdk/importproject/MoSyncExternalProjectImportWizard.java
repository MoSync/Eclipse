/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.mobilesorcery.sdk.importproject;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.wizards.datatransfer.DataTransferMessages;
import org.eclipse.ui.internal.wizards.datatransfer.WizardProjectsImportPage;

/**
 * Standard workbench wizard for importing projects defined
 * outside of the currently defined projects into Eclipse.
 * <p>
 * This class may be instantiated and used without further configuration;
 * this class is not intended to be subclassed.
 * </p>
 * <p>
 * Example:
 * <pre>
 * IWizard wizard = new ExternalProjectImportWizard();
 * wizard.init(workbench, selection);
 * WizardDialog dialog = new WizardDialog(shell, wizard);
 * dialog.open();
 * </pre>
 * During the call to <code>open</code>, the wizard dialog is presented to the
 * user. When the user hits Finish, a project is created with the location
 * specified by the user.
 * </p>
 * @noextend This class is not intended to be subclassed by clients.
 */

public class MoSyncExternalProjectImportWizard extends Wizard implements
        IImportWizard {
    private static final String EXTERNAL_PROJECT_SECTION = "ExternalProjectImportWizard";//$NON-NLS-1$
	private MoSyncWizardProjectsImportPage mainPage;
	private IStructuredSelection currentSelection = null;
	private String initialPath = null;

    /**
     * Constructor for ExternalProjectImportWizard.
     */
    public MoSyncExternalProjectImportWizard() {
    	this(null);
    }

    /**
     * Constructor for ExternalProjectImportWizard.
     *
     * @param initialPath Default path for wizard to import
     * @since 3.5
     */
    public MoSyncExternalProjectImportWizard(String initialPath)
    {
        super();
        this.initialPath = initialPath;
        setNeedsProgressMonitor(true);
        IDialogSettings workbenchSettings = IDEWorkbenchPlugin.getDefault()
        		.getDialogSettings();

		IDialogSettings wizardSettings = workbenchSettings
		        .getSection(EXTERNAL_PROJECT_SECTION);
		if (wizardSettings == null) {
			wizardSettings = workbenchSettings
		            .addNewSection(EXTERNAL_PROJECT_SECTION);
		}
		setDialogSettings(wizardSettings);
    }

    /* (non-Javadoc)
     * Method declared on IWizard.
     */
    @Override
	public void addPages() {
        super.addPages();
		mainPage = new MoSyncWizardProjectsImportPage(
				"wizardExternalProjectsPage", initialPath, currentSelection); //$NON-NLS-1$
        addPage(mainPage);
    }

    /* (non-Javadoc)
     * Method declared on IWorkbenchWizard.
     */
    @Override
	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        setWindowTitle(DataTransferMessages.DataTransfer_importTitle);
        setDefaultPageImageDescriptor(
				IDEWorkbenchPlugin.getIDEImageDescriptor("wizban/importproj_wiz.png")); //$NON-NLS-1$
        this.currentSelection = currentSelection;
    }

    /* (non-Javadoc)
     * Method declared on IWizard.
     */
    @Override
	public boolean performCancel() {
    	mainPage.performCancel();
        return true;
    }

    /* (non-Javadoc)
     * Method declared on IWizard.
     */
    @Override
	public boolean performFinish() {
        return mainPage.createProjects();
    }

}
