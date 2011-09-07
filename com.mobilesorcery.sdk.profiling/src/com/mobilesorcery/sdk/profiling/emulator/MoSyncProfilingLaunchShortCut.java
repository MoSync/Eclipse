/*  Copyright (C) 2010 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.profiling.emulator;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;

import com.mobilesorcery.sdk.core.launch.MoReLauncher;
import com.mobilesorcery.sdk.ui.internal.launch.MoreLaunchShortCut;

public class MoSyncProfilingLaunchShortCut extends MoreLaunchShortCut {

	@Override
    protected ILaunchConfigurationType getConfigType() {
        ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType configType = lm.getLaunchConfigurationType("com.mobilesorcery.profiling.launchconfigurationtype");
        return configType;
    }

	@Override
	protected String getPreferredLauncherId() {
		return MoReLauncher.ID;
	}
}
