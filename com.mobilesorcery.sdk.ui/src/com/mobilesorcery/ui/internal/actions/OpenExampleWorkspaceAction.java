/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

// COPIED & MODIFIED FROM INTERNAL CLASS (WHY INTERNAL? THIS CANNOT BE THAT EXOTIC!?)

package com.mobilesorcery.ui.internal.actions;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;

import com.mobilesorcery.sdk.core.MoSyncTool;

/**
 * Implements the open workspace action. Opens a dialog prompting for a
 * directory and then restarts the IDE on that workspace.
 * 
 * @since 3.0
 */
public class OpenExampleWorkspaceAction extends Action implements IWorkbenchWindowActionDelegate {

    private static final String PROP_VM = "eclipse.vm"; //$NON-NLS-1$

    private static final String PROP_VMARGS = "eclipse.vmargs"; //$NON-NLS-1$

    private static final String PROP_COMMANDS = "eclipse.commands"; //$NON-NLS-1$

    private static final String PROP_EXIT_CODE = "eclipse.exitcode"; //$NON-NLS-1$

    private static final String PROP_EXIT_DATA = "eclipse.exitdata"; //$NON-NLS-1$

    private static final String CMD_DATA = "-data"; //$NON-NLS-1$

    private static final String CMD_VMARGS = "-vmargs"; //$NON-NLS-1$

    private static final String NEW_LINE = "\n"; //$NON-NLS-1$

    private IWorkbenchWindow window;

    private String workspacePath;

    public OpenExampleWorkspaceAction() {
        super();
        String exampleWorkspacePath = MoSyncTool.getDefault().getMoSyncHome().append(new Path("examples")).toOSString(); 
        setWorkspacePath(exampleWorkspacePath);
    }

    public void run() {
        restart(workspacePath);
    }

    public void setWorkspacePath(String workspacePath) {
        this.workspacePath = workspacePath;
    }
    
    /**
     * Restart the workbench using the specified path as the workspace location.
     * 
     * @param path
     *            the location
     * @since 3.3
     */
    private void restart(String path) {
        String command_line = buildCommandLine(path);
        if (command_line == null) {
            return;
        }

        System.setProperty(PROP_EXIT_CODE, Integer.toString(24));
        System.setProperty(PROP_EXIT_DATA, command_line);
        window.getWorkbench().restart();
    }

    /**
     * Create and return a string with command line options for eclipse.exe that
     * will launch a new workbench that is the same as the currently running
     * one, but using the argument directory as its workspace.
     * 
     * @param workspace
     *            the directory to use as the new workspace
     * @return a string of command line options or null on error
     */
    private String buildCommandLine(String workspace) {
        String property = System.getProperty(PROP_VM);
        if (property == null) {
            MessageDialog
                    .openError(
                            window.getShell(),
                            IDEWorkbenchMessages.OpenWorkspaceAction_errorTitle,
                            NLS
                                    .bind(
                                            IDEWorkbenchMessages.OpenWorkspaceAction_errorMessage,
                                            PROP_VM));
            return null;
        }

        StringBuffer result = new StringBuffer(512);
        result.append(property);
        result.append(NEW_LINE);

        // append the vmargs and commands. Assume that these already end in \n
        String vmargs = System.getProperty(PROP_VMARGS);
        if (vmargs != null) {
            result.append(vmargs);
        }

        // append the rest of the args, replacing or adding -data as required
        property = System.getProperty(PROP_COMMANDS);
        if (property == null) {
            result.append(CMD_DATA);
            result.append(NEW_LINE);
            result.append(workspace);
            result.append(NEW_LINE);
        } else {
            // find the index of the arg to replace its value
            int cmd_data_pos = property.lastIndexOf(CMD_DATA);
            if (cmd_data_pos != -1) {
                cmd_data_pos += CMD_DATA.length() + 1;
                result.append(property.substring(0, cmd_data_pos));
                result.append(workspace);
                result.append(property.substring(property.indexOf('\n',
                        cmd_data_pos)));
            } else {
                result.append(CMD_DATA);
                result.append(NEW_LINE);
                result.append(workspace);
                result.append(NEW_LINE);
                result.append(property);
            }
        }

        // put the vmargs back at the very end (the eclipse.commands property
        // already contains the -vm arg)
        if (vmargs != null) {
            result.append(CMD_VMARGS);
            result.append(NEW_LINE);
            result.append(vmargs);
        }

        return result.toString();
    }

    public void dispose() {
        window = null;
    }

    public void init(IWorkbenchWindow window) {
        this.window = window;
    }

    public void run(IAction action) {
        run();
    }

    public void selectionChanged(IAction action, ISelection selection) {
    }
}
