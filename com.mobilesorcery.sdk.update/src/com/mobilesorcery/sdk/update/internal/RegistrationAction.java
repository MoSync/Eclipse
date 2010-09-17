package com.mobilesorcery.sdk.update.internal;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IUpdater;

public class RegistrationAction extends Action implements IWorkbenchWindowActionDelegate {

    public void run() {
        IUpdater updater = CoreMoSyncPlugin.getDefault().getUpdater();
        if (updater != null) {
            updater.register(true);
        }
    }
    
    public void dispose() {
    }

    public void init(IWorkbenchWindow window) {
        setEnabled(CoreMoSyncPlugin.getDefault().getUpdater() != null);
    }

    public void run(IAction action) {
        run();
    }

    public void selectionChanged(IAction action, ISelection selection) {
    }

}
