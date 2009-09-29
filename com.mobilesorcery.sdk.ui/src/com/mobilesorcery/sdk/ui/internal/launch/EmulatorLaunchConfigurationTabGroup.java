package com.mobilesorcery.sdk.ui.internal.launch;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

public class EmulatorLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {

    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] { new MoSyncLaunchParamsTab() };

        for (int i = 0; i < tabs.length; i++) {
            tabs[i].setLaunchConfigurationDialog(dialog);
        }

        setTabs(tabs);
    }

}
