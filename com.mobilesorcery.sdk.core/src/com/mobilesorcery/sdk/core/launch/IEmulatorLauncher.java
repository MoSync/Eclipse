/*  Copyright (C) 2011 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.core.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * <p>An interface for launching mobile apps on different platforms (such as the MoRe
 * emulator, the Android emulator, etc).</p>
 * <p>Currently, only emulators are supported due to the fact that there is already
 * a mechanism to send apps to physical devices via the toolbar.</p>
 * @author Mattias Bybro, mattias.bybro@purplescout.se; mattias@bybro.com
 *
 */
public interface IEmulatorLauncher {

    public static final String EXTENSION_POINT_ID = "com.mobilesorcery.core.launcher";

	public void launch(ILaunchConfiguration launchConfig, String mode, ILaunch launch, int emulatorId, IProgressMonitor monitor) throws CoreException;

	public String getName();

}
