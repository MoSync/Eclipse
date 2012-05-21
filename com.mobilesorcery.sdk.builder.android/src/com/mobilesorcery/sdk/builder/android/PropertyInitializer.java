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
package com.mobilesorcery.sdk.builder.android;

import java.util.Random;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.mobilesorcery.sdk.builder.java.KeystoreCertificateInfo;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IPropertyInitializerDelegate;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PreferenceStorePropertyOwner;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.SecurePropertyException;

public class PropertyInitializer extends AbstractPreferenceInitializer implements IPropertyInitializerDelegate {

    private static final String PREFIX = "android:"; //$NON-NLS-1$
    
    public static final String ANDROID_KEYSTORE_CERT_INFO = PREFIX + "keystore.cert.info"; //$NON-NLS-1$

    public static final String ANDROID_PROJECT_SPECIFIC_KEYS = PREFIX + "proj.spec.keys"; //$NON-NLS-1$
    
    /**
     * The package name, as it's used in the android manifest
     */
    public static final String ANDROID_PACKAGE_NAME = PREFIX + "package.name";
   
    /**
     * The version code, as it's used in the android manifest
     */
    public static final String ANDROID_VERSION_CODE = PREFIX + "version.number";
    
	public static final String ADB_DEBUG_LOG = PREFIX + "adb.debug";

    private static Random rnd = new Random(System.currentTimeMillis());
    
    public PropertyInitializer() {
    }

    public String getDefaultValue(IPropertyOwner p, String key) {
        if (key.equals(ANDROID_PROJECT_SPECIFIC_KEYS)) {
            return PropertyUtil.fromBoolean(false);
        } else if (key.equals(ANDROID_PACKAGE_NAME)) {
            String name = "default_package";
            if (p instanceof MoSyncProject) {
                name = ((MoSyncProject) p).getName();
            }
            
            name = name.replace(' ', '_');
            return "com.mosync.app_" + name;
        } else if (key.equals(ANDROID_VERSION_CODE)) {
            return "1";
        } else {
            return Activator.getDefault().getPreferenceStore().getString(key);
        }
    }

    public void initializeDefaultPreferences() {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        try {
        	store.setDefault(ADB_DEBUG_LOG, false);
			KeystoreCertificateInfo.createDefault().store(ANDROID_KEYSTORE_CERT_INFO,
					new PreferenceStorePropertyOwner(store, true),
					null);
		} catch (SecurePropertyException e) {
			CoreMoSyncPlugin.getDefault().log(e);
		}
    }


}
