package com.mobilesorcery.sdk.profiles.ui.internal.actions;

import org.eclipse.core.resources.IProject;
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
