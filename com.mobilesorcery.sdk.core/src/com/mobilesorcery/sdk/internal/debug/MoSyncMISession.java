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
package com.mobilesorcery.sdk.internal.debug;

import org.eclipse.cdt.debug.mi.core.IMITTY;
import org.eclipse.cdt.debug.mi.core.MIException;
import org.eclipse.cdt.debug.mi.core.MIProcess;
import org.eclipse.cdt.debug.mi.core.MISession;
import org.eclipse.cdt.debug.mi.core.command.CommandFactory;
import org.eclipse.cdt.debug.mi.core.command.MITargetSelect;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;

public class MoSyncMISession extends MISession {

	public MoSyncMISession(MIProcess process, IMITTY pty, int type,
			CommandFactory commandFactory, int timeout) throws MIException {
		super(process, pty, type, commandFactory, timeout);	
	}

	protected void initialize() throws MIException {
		super.initialize();

		// MOSYNC-2163: An intermittent bug that causes connection failed
		// on some computers. Let's just try to reconnect two or three times
		// before giving up.
		int retries = 3;
		while (retries > 0)
		try {
			// MBD also requires us connecting to port 50000 and then
			// an exec-continue needs to be sent to it.
			MITargetSelect selectTarget = getCommandFactory().createMITargetSelect(new String[] { "remote", "localhost:50006" });
		    postCommand(selectTarget);
			selectTarget.getMIInfo();
			retries = 0;
		} catch (MIException e) {
			retries--;
			// Sleep for a short while between retries.
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e1) {
				// Silently ignore.
			}
			if (retries <= 0) {
				throw e;
			}
			CoreMoSyncPlugin.getDefault().getLog().log(new Status(IStatus.INFO, CoreMoSyncPlugin.PLUGIN_ID, "Failed to connect to debug target. Will retry; retries left: " + retries));
		}


		// MIExecContinue start = getCommandFactory().createMIExecContinue();
		// postCommand(start);
		// start.getMIInfo();
	}

	protected String getCLIPrompt() {
		// Until mdb supports -gdb-show prompt
		return "(gdb)";
	}

}
