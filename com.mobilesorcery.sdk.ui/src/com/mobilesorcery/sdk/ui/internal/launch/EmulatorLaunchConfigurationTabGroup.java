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
package com.mobilesorcery.sdk.ui.internal.launch;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;

public class EmulatorLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {

    public void initializeFrom(ILaunchConfiguration configuration) {
        super.initializeFrom(configuration);
    }
    
    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
    	MoSyncLaunchParamsTab launchTab = new MoSyncLaunchParamsTab();
    	launchTab.setMode(mode);
    	launchTab.setBuildConfigurationTypes(true, getBuildConfigurationTypes(true));
    	launchTab.setBuildConfigurationTypes(false, getBuildConfigurationTypes(false));
    	launchTab.setAllowExternalEmulators(allowsExternalEmulators());
    	SourceLookupTab sourceLookup = new SourceLookupTab();
    	
        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] { launchTab, sourceLookup };

        for (int i = 0; i < tabs.length; i++) {
            tabs[i].setLaunchConfigurationDialog(dialog);
        }

        setTabs(tabs);
    }

    /**
     * Returns the build configuration types to show in the "Switch automatically to this"
     * combo box. Clients may override.
     * @param isDebug
     * @return The build configuration types to show, or <code>null</code> for all.
     */
    protected String[] getBuildConfigurationTypes(boolean isDebug) {
        return null;
    }
    
    /**
     * Returns whether this launch configuration UI allows other emulators
     * than the default. Clients may override.
     * @return
     */
    protected boolean allowsExternalEmulators() {
    	return true;
    }

}
