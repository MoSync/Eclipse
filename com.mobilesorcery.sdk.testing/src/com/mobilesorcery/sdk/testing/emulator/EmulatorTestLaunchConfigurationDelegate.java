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
package com.mobilesorcery.sdk.testing.emulator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import com.mobilesorcery.sdk.internal.launch.EmulatorLaunchConfigurationDelegate;
import com.mobilesorcery.sdk.testing.TestManager;

public class EmulatorTestLaunchConfigurationDelegate extends
		EmulatorLaunchConfigurationDelegate {

	// TODO: There should be just a switch or something to indicate what entry point to run - if any!?
	public void launchSync(ILaunchConfiguration launchConfig, String mode, ILaunch launch, int emulatorId, IProgressMonitor monitor) throws CoreException {
		EmulatorTestSession session = new EmulatorTestSession(launchConfig.getName(), launchConfig, emulatorId);
		TestManager.getInstance().addTestSession(session);
		try {
			session.start();
			super.launchSync(launchConfig, mode, launch, emulatorId, monitor);
		} finally {
			session.finish();
		}
	}
}
