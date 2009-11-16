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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.HashMap;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.internal.wizards.newresource.ResourceMessages;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.templates.ITemplate;
import com.mobilesorcery.sdk.core.templates.ProjectTemplate;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.UIUtils;
import com.mobilesorcery.sdk.wizards.Activator;

public class NewMoSyncProjectWizard extends Wizard implements INewWizard {

    private NewMoSyncProjectPage projectPage;
    private TemplateWizardPage templatePage;

    public boolean performFinish() {
        boolean completed = false;

        final ProjectTemplate template = templatePage.getProjectTemplate();

        IRunnableWithProgress createProject = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                try {
                    createProject(monitor, template);
                } catch (CoreException e) {
                    e.printStackTrace();
                    throw new InvocationTargetException(e);
                }
            }
        };

        try {
            getContainer().run(false, true, new WorkspaceModifyDelegatingOperation(createProject));
            completed = true;
        } catch (Exception e) {
            e.printStackTrace();
            // completed is false
        }

        return completed;
    }

    public void init(IWorkbench workbench, IStructuredSelection selection) {
    }

    public void addPages() {
        projectPage = new NewMoSyncProjectPage();
        templatePage = new TemplateWizardPage();
        addPage(projectPage);
        addPage(templatePage);
    }

    private void createProject(IProgressMonitor monitor, ProjectTemplate mainTemplate) throws CoreException {
        IProject project = projectPage.getProjectHandle();

        URI location = null;
        if (!projectPage.useDefaults()) {
            location = projectPage.getLocationURI();
        }

        /*IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProjectDescription description = workspace.newProjectDescription(project.getName());
        description.setLocationURI(location);

        CreateProjectOperation op = new CreateProjectOperation(description, "Create Project");
        
        try {
            PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().execute(op, monitor,
                    WorkspaceUndoUtil.getUIInfoAdapter(getShell()));
        } catch (ExecutionException e) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
        }

        MoSyncCore.addNatureToProject(project);*/
        
        MoSyncProject mosyncProject = MosyncUIPlugin.createProject(project, location, monitor);

        if (mainTemplate != null) {
        	IFile mainFile = mainTemplate.initalizeProject(monitor, mosyncProject);
        	if (mainFile != null) {
        		UIUtils.openResource(PlatformUI.getWorkbench(), mainFile);
        	}
        	
            IResource revealThis = mainFile == null ? project : mainFile;
            BasicNewResourceWizard.selectAndReveal(revealThis, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
        }
    }
}
