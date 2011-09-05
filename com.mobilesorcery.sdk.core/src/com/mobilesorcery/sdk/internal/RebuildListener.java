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
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.NameSpacePropertyOwner;
import com.mobilesorcery.sdk.internal.cdt.MoSyncIncludePathContainer;
import com.mobilesorcery.sdk.internal.cdt.MoSyncPathInitializer;
import com.mobilesorcery.sdk.profiles.IProfile;

/**
 * A listener that makes sure to rebuild a project if certain events occur.
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 *
 */
public class RebuildListener implements PropertyChangeListener {

    @Override
	public void propertyChange(PropertyChangeEvent event) {
    	boolean doTouch = false;
    	boolean updateProjectPaths = shouldUpdatePaths(event);
        if (updateProjectPaths) {
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
                doTouch = profile != null;
            }
        } else if (MoSyncProject.BUILD_CONFIGURATION_CHANGED == event.getPropertyName()) {
        	doTouch = true;
        }

        if (doTouch) {
            // Make sure we rebuild when necessary (at a later point)
            Object source = event.getSource();
            if (source instanceof MoSyncProject) {
                try {
                    ((MoSyncProject) source).getWrappedProject().touch(null);
                } catch (CoreException e) {
                    CoreMoSyncPlugin.getDefault().log(e);
                }
            }
        }
    }

	private boolean shouldUpdatePaths(PropertyChangeEvent event) {
		return MoSyncProject.TARGET_PROFILE_CHANGED == event.getPropertyName() ||
				MoSyncBuilder.ADDITIONAL_INCLUDE_PATHS.equals(NameSpacePropertyOwner.getKey(event.getPropertyName())) ||
				MoSyncBuilder.IGNORE_DEFAULT_INCLUDE_PATHS.equals(NameSpacePropertyOwner.getKey(event.getPropertyName()));
	}

}
