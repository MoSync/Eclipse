package com.mobilesorcery.sdk.internal.preferences;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncTool;

public class MoSyncToolPreferenceInitializer extends AbstractPreferenceInitializer {

    public MoSyncToolPreferenceInitializer() {
    }

    public void initializeDefaultPreferences() {
        boolean envOk = MoSyncTool.isValidHome(MoSyncTool.getMoSyncHomeFromEnv());
        boolean guessOk = MoSyncTool.isValidHome(MoSyncTool.guessHome());
        
        String home = "";
        
        if (envOk) {
            home = MoSyncTool.getMoSyncHomeFromEnv().toOSString();
        } else if (guessOk) {
            home = MoSyncTool.guessHome().toOSString();
        }
        
        CoreMoSyncPlugin.getDefault().getPreferenceStore().setDefault(MoSyncTool.MOSYNC_HOME_PREF, home);
        CoreMoSyncPlugin.getDefault().getPreferenceStore().setDefault(MoSyncTool.MO_SYNC_HOME_FROM_ENV_PREF, envOk);
    }

}
