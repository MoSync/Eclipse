package com.mobilesorcery.sdk.builder.blackberry;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.mobilesorcery.sdk.builder.java.KeystoreCertificateInfo;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.PreferenceStorePropertyOwner;
import com.mobilesorcery.sdk.core.SecurePropertyException;

public class PropertyInitializer extends AbstractPreferenceInitializer {

    public void initializeDefaultPreferences() {
        IPreferenceStore store = BlackBerryPlugin.getDefault().getPreferenceStore();
        try {
			KeystoreCertificateInfo.createDefault().store(BlackBerryPlugin.BLACKBERRY_SIGNING_INFO,
					new PreferenceStorePropertyOwner(store, true),
			        null);
		} catch (SecurePropertyException e) {
			CoreMoSyncPlugin.getDefault().log(e);
		}
    }
}
