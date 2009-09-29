package com.mobilesorcery.ui.internal.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.mobilesorcery.sdk.core.MoSyncNature;

public class ConvertToMoSyncProject implements IObjectActionDelegate {

    private ISelection selection;

    public void setActivePart(IAction action, IWorkbenchPart part) {
    }

    public void run(IAction action) {
        if (selection instanceof IStructuredSelection) {
            Object first = ((IStructuredSelection)selection).getFirstElement();
            if (first instanceof IProject) {
                IProject project = (IProject) first;
                MoSyncNature.addNatureToProject(project);
            }
        }
    }

    public void selectionChanged(IAction action, ISelection selection) {
        this.selection = selection;        
    }

}
