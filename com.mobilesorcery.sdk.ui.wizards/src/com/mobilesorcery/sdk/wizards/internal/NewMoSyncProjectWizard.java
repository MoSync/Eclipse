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

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.w3c.dom.css.Counter;

import com.mobilesorcery.sdk.core.MoSyncNature;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.stats.CounterVariable;
import com.mobilesorcery.sdk.core.stats.Stats;
import com.mobilesorcery.sdk.core.templates.IProjectTemplateExtension;
import com.mobilesorcery.sdk.core.templates.ProjectTemplate;
import com.mobilesorcery.sdk.core.templates.TemplateManager;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.UIUtils;
import com.mobilesorcery.sdk.wizards.Activator;

public class NewMoSyncProjectWizard extends Wizard implements INewWizard {

    private NewMoSyncProjectPage projectPage;
    private TemplateWizardPage templatePage;

    @Override
	public boolean performFinish() {
        boolean completed = false;

        final ProjectTemplate template = templatePage.getProjectTemplate();

        IRunnableWithProgress createProject = new IRunnableWithProgress() {
            @Override
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

    @Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
    }

    @Override
	public void addPages() {
        projectPage = new NewMoSyncProjectPage();
        templatePage = new TemplateWizardPage();
        addPage(templatePage);
        addPage(projectPage);
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
        	IFile mainFile = mainTemplate.initializeProject(monitor, mosyncProject);
        	IProjectTemplateExtension ext = TemplateManager.getDefault().getExtensionForType(mainTemplate.getType());
        	if (ext != null) {
        		ext.configureProject(mosyncProject);
        	}
        	if (mainFile != null) {
        		UIUtils.openResource(PlatformUI.getWorkbench(), mainFile);
        	}

        	Stats.getStats().getVariables().get(CounterVariable.class, "project-count-" + mainTemplate.getId()).inc();
            IResource revealThis = mainFile == null ? project : mainFile;
            BasicNewResourceWizard.selectAndReveal(revealThis, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
        }
        Stats.getStats().getVariables().get(CounterVariable.class, "project-count").inc();
        configureProject(mosyncProject);
        // Just to make sure we have the correct paths right after creation.
        MoSyncNature.modifyIncludePaths(mosyncProject.getWrappedProject());
    }

	protected void configureProject(MoSyncProject mosyncProject) throws CoreException {
		// Clients may override
	}

}
