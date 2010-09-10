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

import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;

public class ShowHTMLAction extends Action implements IWorkbenchWindowActionDelegate {

    public void dispose() {
    }

    public void init(IWorkbenchWindow window) {
        
    }

    public void run(IAction action) {
        try {
            URL url = new URL(Util.toGetUrl("http://www.mosync.com/content/documentation", MosyncUIPlugin.getDefault().getVersionParameters(true)));
            PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(url);
        } catch (Exception e) {
            StatusManager.getManager().handle(new Status(IStatus.ERROR, MosyncUIPlugin.PLUGIN_ID, "Could not show help", e), StatusManager.SHOW);
        }
    }

    public void selectionChanged(IAction action, ISelection selection) {
        
    }

}
