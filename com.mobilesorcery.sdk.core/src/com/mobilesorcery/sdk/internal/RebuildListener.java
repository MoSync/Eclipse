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
package com.mobilesorcery.sdk.internal;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.internal.cdt.MoSyncIncludePathContainer;
import com.mobilesorcery.sdk.internal.cdt.MoSyncPathInitializer;
import com.mobilesorcery.sdk.profiles.IProfile;

public class RebuildListener implements PropertyChangeListener {
	
    public void propertyChange(PropertyChangeEvent event) {
        if (MoSyncProject.TARGET_PROFILE_CHANGED == event.getPropertyName()) {
            Object source = event.getSource();
            if (source instanceof MoSyncProject) {
                MoSyncProject project = (MoSyncProject) source;

                // This line will make sure to refresh include paths and notify
            	// everyone about it.
                ICProject cProject = CoreModel.getDefault().create(project.getWrappedProject());

                try {
					MoSyncPathInitializer.getInstance().initialize(MoSyncIncludePathContainer.CONTAINER_ID, cProject);
				} catch (CoreException e) {
					CoreMoSyncPlugin.getDefault().log(e);
				}

                IProfile profile = project.getTargetProfile();
                if (profile != null) {
                    runBuildJob(project, profile);
                }
            }
        }
    }

    private void runBuildJob(final MoSyncProject project, final IProfile targetProfile) {
        Job job = new Job("Building for target profile") {
            protected IStatus run(IProgressMonitor monitor) {
                try {
                	monitor.beginTask("Building", 2);
                	project.getWrappedProject().refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 1));
                    new MoSyncBuilder().fullBuild(project.getWrappedProject(), MoSyncBuilder.getActiveVariant(project, false), false, new SubProgressMonitor(monitor, 1));
                } catch (CoreException e) {
                    return new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, MessageFormat.format(
                            "Could not build for target {0}. Root cause: {1}", targetProfile, e.getMessage()), e);
                }
                
                return Status.OK_STATUS;
            }            
            
        };
        
        job.setUser(true);
        job.schedule();
    }

}
