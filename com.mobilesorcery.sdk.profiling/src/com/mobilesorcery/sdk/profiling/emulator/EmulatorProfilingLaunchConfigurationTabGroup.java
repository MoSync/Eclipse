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

import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

import com.mobilesorcery.sdk.ui.internal.launch.EmulatorLaunchConfigurationTabGroup;

public class EmulatorProfilingLaunchConfigurationTabGroup extends EmulatorLaunchConfigurationTabGroup {

	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		super.createTabs(dialog, mode);
		ILaunchConfigurationTab[] originalTabs = getTabs();
		ILaunchConfigurationTab[] newTabs = new ILaunchConfigurationTab[originalTabs.length + 1];
		System.arraycopy(originalTabs, 0, newTabs, 0, originalTabs.length);
		newTabs[newTabs.length - 1] = new EmulatorProfilingLaunchConfigurationTab();
		setTabs(newTabs);
	}
}
