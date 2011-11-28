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


import java.util.Arrays;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.mobilesorcery.sdk.core.BuildVariant;
import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncBuildJob;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.ui.Activator;

public class FinalizeForProfileAction extends Action {

    private ISelection selection;
    private MoSyncProject project;

    public FinalizeForProfileAction() {
        super("Finalize for this profile", Activator.getDefault().getImageRegistry().getDescriptor(Activator.BUILD_FOR_PROFILE_IMAGE));
    }

    @Override
	public void run() {
        if (selection instanceof IStructuredSelection) {
            Object selected = ((IStructuredSelection)selection).getFirstElement();
            if (selected instanceof IProfile && project != null) {
                //FinalizerParser.autoSwitchConfiguration(project);
                IProfile profile = (IProfile) selected;
                IBuildConfiguration cfg = project.getActiveBuildConfiguration();
                IBuildVariant variant = new BuildVariant(profile, cfg == null ? null : cfg.getId());
                IBuildSession session = MoSyncBuilder.createFinalizerBuildSession(Arrays.asList(variant));
                MoSyncBuildJob job = new MoSyncBuildJob(project, session, variant);
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
