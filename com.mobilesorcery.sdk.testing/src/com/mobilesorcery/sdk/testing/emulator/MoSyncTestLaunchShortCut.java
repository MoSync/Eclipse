package com.mobilesorcery.sdk.testing.emulator;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;

import com.mobilesorcery.sdk.ui.internal.launch.MoreLaunchShortCut;

public class MoSyncTestLaunchShortCut extends MoreLaunchShortCut {

    protected ILaunchConfigurationType getConfigType() {
        ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType configType = lm.getLaunchConfigurationType("com.mobilesorcery.test.launchconfigurationtype");
        return configType;
    }

}
