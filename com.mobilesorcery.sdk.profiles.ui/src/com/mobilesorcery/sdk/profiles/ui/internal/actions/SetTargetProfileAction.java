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
package com.mobilesorcery.sdk.profiles.ui.internal.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.ui.Activator;

public class SetTargetProfileAction extends Action {

    private ISelection selection;
    private MoSyncProject project;
    
    public SetTargetProfileAction() {
        super(Messages.SetTargetProfileAction_SetTargetPhone, Activator.getDefault().getImageRegistry().getDescriptor(Activator.TARGET_PHONE_IMAGE));
    }

    public void run() {
        if (selection instanceof IStructuredSelection) {
            Object selected = ((IStructuredSelection)selection).getFirstElement();
            if (selected instanceof IProfile && project != null) {
                MoSyncProject msProject = project;
                IProfile profile = (IProfile) selected;
                msProject.setTargetProfile(profile);
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
