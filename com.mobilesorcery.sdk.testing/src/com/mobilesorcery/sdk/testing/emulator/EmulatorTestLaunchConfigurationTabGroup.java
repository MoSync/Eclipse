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

import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.testing.TestPlugin;
import com.mobilesorcery.sdk.ui.internal.launch.EmulatorLaunchConfigurationTabGroup;

public class EmulatorTestLaunchConfigurationTabGroup extends
		EmulatorLaunchConfigurationTabGroup {

    protected String[] getBuildConfigurationTypes(boolean isDebug) {
        return new String[] { isDebug ? IBuildConfiguration.DEBUG_TYPE : IBuildConfiguration.RELEASE_TYPE, TestPlugin.TEST_BUILD_CONFIGURATION_TYPE };
    }
    
}
