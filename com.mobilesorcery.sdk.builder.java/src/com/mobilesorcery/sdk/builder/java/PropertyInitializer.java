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
package com.mobilesorcery.sdk.builder.java;

import java.util.ArrayList;
import java.util.Random;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IPropertyInitializerDelegate;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.PreferenceStorePropertyOwner;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.SecurePropertyException;

public class PropertyInitializer extends AbstractPreferenceInitializer implements IPropertyInitializerDelegate {

    private static final String PREFIX = "javame:"; //$NON-NLS-1$

    public static final String JAVAME_KEYSTORE_CERT_INFOS = PREFIX + "keystore.cert.infos"; //$NON-NLS-1$

    public static final String JAVAME_PROJECT_SPECIFIC_KEYS = PREFIX + "proj.spec.keys"; //$NON-NLS-1$

    public static final String JAVAME_DO_SIGN = PREFIX + "do.sign"; //$NON-NLS-1$

    private static Random rnd = new Random(System.currentTimeMillis());

    public PropertyInitializer() {
    }

    @Override
	public String getDefaultValue(IPropertyOwner p, String key) {
        if (key.equals(JAVAME_PROJECT_SPECIFIC_KEYS)) {
            return PropertyUtil.fromBoolean(false);
        } else if (key.equals(JAVAME_DO_SIGN)) {
        	return PropertyUtil.fromBoolean(false);
        }
        return null;
    }

    @Override
	public void initializeDefaultPreferences() {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        // By default, java me apps are NOT signed.
        try {
        	store.setDefault(JAVAME_DO_SIGN, false);
        	KeystoreCertificateInfo defaultCert = KeystoreCertificateInfo.createDefault();
        	ArrayList<KeystoreCertificateInfo> defaultCerts = new ArrayList<KeystoreCertificateInfo>();
        	defaultCerts.add(defaultCert);
			KeystoreCertificateInfo.store(defaultCerts,
					JAVAME_KEYSTORE_CERT_INFOS,
					new PreferenceStorePropertyOwner(store, true),
					CoreMoSyncPlugin.getDefault().getSecureProperties());
		} catch (SecurePropertyException e) {
			CoreMoSyncPlugin.getDefault().log(e);
		}
    }


}
