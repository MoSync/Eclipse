package com.mobilesorcery.sdk.product.intro.actions;

import java.util.Properties;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.IIntroAction;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncTool;

public class SwitchWorkspaceAction implements IIntroAction {

	public void run(IIntroSite site, Properties params) {
		String ws = params.getProperty("ws");
		//boolean suppressUpdates = false;
		if ("example-ws".equalsIgnoreCase(ws)) {
		    IPath exampleWS = MoSyncTool.getDefault().getMoSyncExamplesWorkspace();
		    ws = exampleWS.toOSString();
		    //suppressUpdates = true;
		}
		
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		if (MessageDialog.openConfirm(shell, "Switch workspace", "This will restart the MoSync IDE (please note that it will take a few moments).")) {
		    restart(ws);    
		}
	}
	
	// Some ridiculous copy-n-paste from eclipse.
	private static final String PROP_VM = "eclipse.vm"; //$NON-NLS-1$

	private static final String PROP_VMARGS = "eclipse.vmargs"; //$NON-NLS-1$

	private static final String PROP_COMMANDS = "eclipse.commands"; //$NON-NLS-1$

	private static final String PROP_EXIT_CODE = "eclipse.exitcode"; //$NON-NLS-1$

	private static final String PROP_EXIT_DATA = "eclipse.exitdata"; //$NON-NLS-1$

	private static final String CMD_DATA = "-data"; //$NON-NLS-1$

	private static final String CMD_VMARGS = "-vmargs"; //$NON-NLS-1$

	private static final String NEW_LINE = "\n"; //$NON-NLS-1$

	public void restart(String path/*, boolean suppressUpdates*/) {
		String command_line = buildCommandLine(path/*, suppressUpdates*/);
		if (command_line == null) {
			return;
		}

		if (CoreMoSyncPlugin.getDefault().isDebugging()) {
			CoreMoSyncPlugin.trace("Restarting command line. Command line: {0}", command_line);
		}

		System.setProperty(PROP_EXIT_CODE, Integer.toString(24));
		System.setProperty(PROP_EXIT_DATA, command_line);
		PlatformUI.getWorkbench().restart();
	}

	public String buildCommandLine(String workspace/*, boolean suppressUpdates*/) {
		String property = System.getProperty(PROP_VM);
		if (property == null) {
			MessageDialog
					.openError(
							PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
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
		/*if (suppressUpdates) {
		    property = property == null ? "" : property;
		    property += "-suppress-updates";
		    property += NEW_LINE;
		}*/
		
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




}
