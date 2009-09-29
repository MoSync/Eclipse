package com.mobilesorcery.ui.internal.actions;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

import com.mobilesorcery.sdk.core.MoSyncTool;

public class ShowHTMLAction extends Action implements IWorkbenchWindowActionDelegate {

    public void dispose() {
    }

    public void init(IWorkbenchWindow window) {
        
    }

    public void run(IAction action) {
        // Quasi-hard-coded html help file location
        Program program = Program.findProgram("html");
        IPath helpFile = MoSyncTool.getDefault().getMoSyncHome().append(new Path("docs/index.html"));
        program.execute(helpFile.toFile().getAbsolutePath());
    }

    public void selectionChanged(IAction action, ISelection selection) {
        
    }

}
