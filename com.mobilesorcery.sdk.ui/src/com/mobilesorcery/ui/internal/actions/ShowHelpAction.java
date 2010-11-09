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

import com.mobilesorcery.sdk.ui.MosyncUIPlugin;

public class ShowHelpAction extends Action implements IWorkbenchWindowActionDelegate, IExecutableExtension {

    private String helpResource;
    private Boolean showInExternalBrowser;

    public ShowHelpAction() {
        
    }
    
    public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
        if (data instanceof Map) {
            helpResource = (String) ((Map) data).get("resource");
            showInExternalBrowser = Boolean.parseBoolean((String) ((Map) data).get("showInExternalBrowser"));
        }
    }

    public void dispose() {
    }

    public void init(IWorkbenchWindow iworkbenchwindow) {
    }

    public void run(IAction action) {
        MosyncUIPlugin.getDefault().showHelp(helpResource, showInExternalBrowser);
    }

    public void selectionChanged(IAction action, ISelection selection) {
    }

}
