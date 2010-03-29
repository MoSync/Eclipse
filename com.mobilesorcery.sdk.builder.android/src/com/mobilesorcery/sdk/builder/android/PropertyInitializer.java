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

import com.mobilesorcery.sdk.core.IPropertyInitializerDelegate;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.Util;

public class PropertyInitializer extends AbstractPreferenceInitializer implements IPropertyInitializerDelegate {

    private static final String PREFIX = "android:"; //$NON-NLS-1$
    
    public static final String ANDROID_KEYSTORE = PREFIX + "keystore"; //$NON-NLS-1$
    
    public static final String ANDROID_PASS_STORE = PREFIX + "pass.store"; //$NON-NLS-1$

    public static final String ANDROID_PASS_KEY = PREFIX + "pass.key"; //$NON-NLS-1$

    public static final String ANDROID_PROJECT_SPECIFIC_KEYS = PREFIX + "proj.spec.keys"; //$NON-NLS-1$

    public static final String ANDROID_ALIAS = PREFIX + "key.alias";

    private static Random rnd = new Random(System.currentTimeMillis());
    
    public PropertyInitializer() {
    }

    public String getDefaultValue(IPropertyOwner p, String key) {
        if (key.equals(ANDROID_PROJECT_SPECIFIC_KEYS)) {
            return PropertyUtil.fromBoolean(false);
        } else {
            return Activator.getDefault().getPreferenceStore().getString(key);
        }
    }

    public void initializeDefaultPreferences() {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        store.setDefault(ANDROID_KEYSTORE, MoSyncTool.getDefault().getMoSyncHome().append("etc/mosync.keystore").toOSString());
        store.setDefault(ANDROID_PASS_KEY, "default");
        store.setDefault(ANDROID_PASS_STORE, "default");
        store.setDefault(ANDROID_ALIAS, "mosync.keystore");
    }


}
