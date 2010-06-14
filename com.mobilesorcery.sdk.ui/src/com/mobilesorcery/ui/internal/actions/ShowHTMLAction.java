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

import java.text.MessageFormat;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.mobilesorcery.sdk.core.MoSyncTool;

public class ShowHTMLAction extends Action implements IWorkbenchWindowActionDelegate {

    public void dispose() {
    }

    public void init(IWorkbenchWindow window) {
        
    }

    public void run(IAction action) {
        // Quasi-hard-coded html help file location
        Program program = Program.findProgram("html");
        String registrationKey = MoSyncTool.getDefault().getRegistrationKey();
        int buildNumber = MoSyncTool.getDefault().getCurrentBinaryVersion();
        //IPath helpFile = MoSyncTool.getDefault().getMoSyncHome().append(new Path("docs/index.html"));
        //program.execute(helpFile.toFile().getAbsolutePath());
        program.execute(MessageFormat.format(
                "http://www.mosync.com/content/documentation?cc={0}&r={1}",
                registrationKey,
                Integer.toString(buildNumber)));
    }

    public void selectionChanged(IAction action, ISelection selection) {
        
    }

}
