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
