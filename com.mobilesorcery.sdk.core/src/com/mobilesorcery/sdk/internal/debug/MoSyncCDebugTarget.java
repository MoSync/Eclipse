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

import org.eclipse.cdt.core.IAddressFactory;
import org.eclipse.cdt.core.IBinaryParser.IBinaryObject;
import org.eclipse.cdt.debug.core.CDebugCorePlugin;
import org.eclipse.cdt.debug.core.cdi.model.ICDITarget;
import org.eclipse.cdt.debug.internal.core.CBreakpointManager;
import org.eclipse.cdt.debug.internal.core.model.CDebugTarget;
import org.eclipse.cdt.utils.Addr32Factory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;

public class MoSyncCDebugTarget extends CDebugTarget {

	private IAddressFactory addressFactory = new Addr32Factory();
	private MoSyncCBreakPointManager overriddenBreakpointManager;

	public MoSyncCDebugTarget(ILaunch launch, IProject project,
			ICDITarget cdiTarget, String name, IProcess debuggeeProcess,
			IBinaryObject file, boolean allowsTerminate,
			boolean allowsDisconnect) {
		super(launch, project, cdiTarget, name, debuggeeProcess, file,
				allowsTerminate, allowsDisconnect);
	}

	public static IDebugTarget newDebugTarget(final ILaunch launch,
			final IProject project, final ICDITarget cdiTarget,
			final String name, final IProcess debuggeeProcess,
			final IBinaryObject file, final boolean allowTerminate,
			final boolean allowDisconnect, final String stopSymbol,
			final boolean resumeTarget) throws DebugException {
		final IDebugTarget[] target = new IDebugTarget[1];
		IWorkspaceRunnable r = new IWorkspaceRunnable() {

			public void run(IProgressMonitor m) throws CoreException {
				target[0] = new MoSyncCDebugTarget(launch, project, cdiTarget,
						name, debuggeeProcess, file, allowTerminate,
						allowDisconnect);
				((CDebugTarget) target[0]).start(stopSymbol, resumeTarget);
			}
		};
		try {
			ResourcesPlugin.getWorkspace().run(r, null);
		} catch (CoreException e) {
			CDebugCorePlugin.log(e);
			throw new DebugException(e.getStatus());
		}
		return target[0];
	}
	
	public synchronized CBreakpointManager getBreakpointManager() {
		if (overriddenBreakpointManager == null) {
			overriddenBreakpointManager = new MoSyncCBreakPointManager(this);
		}
		
		return overriddenBreakpointManager;
	}
	
	public IAddressFactory getAddressFactory() {
		return addressFactory;
	}

}
