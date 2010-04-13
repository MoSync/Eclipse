/*  Copyright (C) 2010 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.profiles.ui.internal.actions;

import java.text.MessageFormat;
import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.mobilesorcery.sdk.core.BuildVariant;
import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.ui.Activator;

public class FinalizeForProfileAction extends Action {

    class BuildJob extends Job {
        private IBuildVariant variant;
        private MoSyncProject project;

        public BuildJob(MoSyncProject project, IBuildVariant variant) {
            super(MessageFormat.format("Finalizing project {0} for profile {1}", project.getName(), variant.getProfile()));
            this.variant = variant;
            this.project = project;
            setUser(true);
        }

        protected IStatus run(IProgressMonitor monitor) {
            try {
                new MoSyncBuilder().fullBuild(project.getWrappedProject(), MoSyncBuilder.createFinalizerBuildSession(Arrays.asList(variant)), variant, null, monitor);
            } catch (OperationCanceledException e) {
                return Status.CANCEL_STATUS;
            } catch (CoreException e) {
                return e.getStatus();
            }
            
            return Status.OK_STATUS;
        }        
    }
    
    private ISelection selection;
    private MoSyncProject project;

    public FinalizeForProfileAction() {
        super("Finalize for this profile", Activator.getDefault().getImageRegistry().getDescriptor(Activator.BUILD_FOR_PROFILE_IMAGE));
    }
    
    public void run() {
        if (selection instanceof IStructuredSelection) {
            Object selected = ((IStructuredSelection)selection).getFirstElement();
            if (selected instanceof IProfile && project != null) {
                IProfile profile = (IProfile) selected;
                IBuildConfiguration cfg = project.getActiveBuildConfiguration();
                BuildVariant variant = new BuildVariant(profile, cfg == null ? null : cfg.getId(), true);
                BuildJob job = new BuildJob(project, variant);
                job.schedule();
            }
        }
    }
          
    public void setCurrentProject(MoSyncProject currentProject) {
        this.project = currentProject;
    }
    
    public void setSelection(ISelection selection) {
        this.selection = selection;
    }
}
