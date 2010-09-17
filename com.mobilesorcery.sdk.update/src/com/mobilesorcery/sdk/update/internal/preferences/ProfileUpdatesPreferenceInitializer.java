package com.mobilesorcery.sdk.update.internal.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;

import com.mobilesorcery.sdk.update.MosyncUpdatePlugin;
import com.mobilesorcery.sdk.update.internal.DefaultUpdater2;

public class ProfileUpdatesPreferenceInitializer extends AbstractPreferenceInitializer {

    public void initializeDefaultPreferences() {
        MosyncUpdatePlugin.getDefault().getPreferenceStore().setDefault(DefaultUpdater2.SHOW_CONNECTION_FAILED_POPUP, MessageDialogWithToggle.ALWAYS);
    }

}
