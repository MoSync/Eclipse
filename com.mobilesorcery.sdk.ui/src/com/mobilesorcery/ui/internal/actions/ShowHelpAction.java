package com.mobilesorcery.ui.internal.actions;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

public class ShowHelpAction extends Action implements IWorkbenchWindowActionDelegate, IExecutableExtension {

    private String helpResource;

    public ShowHelpAction() {
        
    }
    
    public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
        if (data instanceof Map) {
            helpResource = (String) ((Map) data).get("resource");
        }
    }

    public void dispose() {
    }

    public void init(IWorkbenchWindow iworkbenchwindow) {
    }

    public void run(IAction action) {
        if (helpResource != null) {
            PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(helpResource);            
        }
    }

    public void selectionChanged(IAction action, ISelection selection) {
    }

}
