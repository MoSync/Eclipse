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
package com.mobilesorcery.sdk.internal.preferences;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncTool;

public class MoSyncToolPreferenceInitializer extends AbstractPreferenceInitializer {

    public MoSyncToolPreferenceInitializer() {
    }

    public void initializeDefaultPreferences() {
        IPath env = MoSyncTool.getMoSyncHomeFromEnv();
        boolean envOk = env != null && MoSyncTool.isValidHome(env);
        
        String home = "";
        
        if (envOk) {
            home = MoSyncTool.getMoSyncHomeFromEnv().toOSString();
        } 
        
        CoreMoSyncPlugin.getDefault().getPreferenceStore().setDefault(MoSyncTool.MOSYNC_HOME_PREF, home);
        CoreMoSyncPlugin.getDefault().getPreferenceStore().setDefault(MoSyncTool.MO_SYNC_HOME_FROM_ENV_PREF, envOk);
    }

}
