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

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;

import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.testing.project.MoSyncProjectTestManager;
import com.mobilesorcery.sdk.ui.internal.launch.MoreLaunchShortCut;

public class MoSyncTestLaunchShortCut extends MoreLaunchShortCut {

    protected ILaunchConfigurationType getConfigType() {
        ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType configType = lm.getLaunchConfigurationType("com.mobilesorcery.test.launchconfigurationtype");
        return configType;
    }

    protected String getInitialConfigurationId(IProject project, String mode) {
        boolean isDebug = "debug".equals(mode);
        List<IBuildConfiguration> testCfgs = new MoSyncProjectTestManager(MoSyncProject.create(project)).getTestConfigs(isDebug);
        if (testCfgs.size() > 0) {
            return testCfgs.get(0).getId();
        }
        
        return null;
    }
}
