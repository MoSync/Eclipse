package com.mobilesorcery.sdk.builder.blackberry;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.mobilesorcery.sdk.builder.java.KeystoreCertificateInfo;

public class PropertyInitializer extends AbstractPreferenceInitializer {

    public void initializeDefaultPreferences() {
        IPreferenceStore store = BlackBerryPlugin.getDefault().getPreferenceStore();
        store.setDefault(BlackBerryPlugin.BLACKBERRY_SIGNING_INFO, KeystoreCertificateInfo.unparse(KeystoreCertificateInfo.createDefault()));
    }
}
