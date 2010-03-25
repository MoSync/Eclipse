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
package com.mobilesorcery.sdk.builder.s60;

import java.util.Random;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.mobilesorcery.sdk.core.IPropertyInitializerDelegate;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.Util;

/**
 * A shared preference/property initializer
 * @author Mattias Bybro
 *
 */
public class PropertyInitializer extends AbstractPreferenceInitializer implements IPropertyInitializerDelegate {

    private static final String PREFIX = "symbian.uids:"; //$NON-NLS-1$

    private static final String UID_PREFIX = PREFIX + "uid."; //$NON-NLS-1$

	public static final String S60V2_UID = UID_PREFIX + "s60v2uid"; //$NON-NLS-1$
    
    public static final String S60V3_UID = UID_PREFIX + "s60v3uid"; //$NON-NLS-1$
    
    public static final String S60_KEY_FILE = PREFIX + "key.file"; //$NON-NLS-1$
    
    public static final String S60_CERT_FILE = PREFIX + "cert.file"; //$NON-NLS-1$

    public static final String S60_PASS_KEY = PREFIX + "pass.key"; //$NON-NLS-1$

    public static final String S60_PROJECT_SPECIFIC_KEYS = PREFIX + "proj.spec.keys"; //$NON-NLS-1$

    private static Random rnd = new Random(System.currentTimeMillis());
    
	public PropertyInitializer() {
	}

	public String getDefaultValue(IPropertyOwner p, String key) {
		if (key.startsWith(UID_PREFIX)) {		    
			String value = generateUID(key);
			// We'll actually set the value.
			p.initProperty(key, value);
			
			return value;
		} else if (key.equals(S60_PROJECT_SPECIFIC_KEYS)) {
            return PropertyUtil.fromBoolean(false);
        } else if (key.equals(S60_KEY_FILE) || key.equals(S60_CERT_FILE) || key.equals(S60_PASS_KEY)) {
            return Activator.getDefault().getPreferenceStore().getString(key);
        }
		
		return null;
	}


    public static String generateUID(String property) {
        long startRange = getStartOfRange(property);
        String uid = Long.toHexString(startRange + rnd.nextInt(getLengthOfRange(property)));        
        uid = Util.fill('0', 8 - uid.length()) + uid;
        return "0x" + uid;         //$NON-NLS-1$
    }
    
    public static long getStartOfRange(String property) {
    	return S60V3_UID == property ? 0xE0000000L : 0L;	
    }
    
    public static int getLengthOfRange(String property) {
    	return 0x0FFFFFFF;
    }

    public void initializeDefaultPreferences() {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        store.setDefault(S60_KEY_FILE, DefaultKeyInitializer.getDefaultKeyFile().getAbsolutePath());
        store.setDefault(S60_CERT_FILE, DefaultKeyInitializer.getDefaultCertFile().getAbsolutePath());
        store.setDefault(S60_PASS_KEY, DefaultKeyInitializer.DEFAULT_PASS_KEY);
    }
}
