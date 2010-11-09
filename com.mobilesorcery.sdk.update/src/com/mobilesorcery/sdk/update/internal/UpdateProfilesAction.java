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
package com.mobilesorcery.sdk.update.internal;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IUpdater;

public class UpdateProfilesAction extends Action implements IWorkbenchWindowActionDelegate {
 
    private IWorkbenchWindow window;

    public void run() {
        IUpdater updater = getUpdater();
        if (updater != null) {
            updater.update(true);
        }
    }
    
    private IUpdater getUpdater() {
        IUpdater updater = CoreMoSyncPlugin.getDefault().getUpdater();
        return updater;
    }
    public void dispose() {
    }
    
    public void init(IWorkbenchWindow window) {
        this.window = window;
        setEnabled(getUpdater() != null);
    }

    public void run(IAction action) {
        run();
    }

    public void selectionChanged(IAction action, ISelection selection) {
        
    }

}
