package com.mobilesorcery.sdk.builder.iphoneos;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.mobilesorcery.sdk.core.IPropertyInitializerDelegate;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.PropertyUtil;

public class PropertyInitializer extends AbstractPreferenceInitializer implements IPropertyInitializerDelegate {

    private static final String PREFIX = "iphone:"; //$NON-NLS-1$
    
    public static final String IPHONE_CERT = PREFIX + "cert"; //$NON-NLS-1$

	public static final String IPHONE_PROJECT_SPECIFIC_CERT = IPHONE_CERT + "proj.spec"; //$NON-NLS-1$

	@Override
	public String getDefaultValue(IPropertyOwner p, String key) {
		if (IPHONE_PROJECT_SPECIFIC_CERT.equals(key)) {
			return PropertyUtil.fromBoolean(false);
		} else {
			return Activator.getDefault().getPreferenceStore().getString(key);
		}
	}

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		// The default cert; will also match user specific dev certs.
		store.setDefault(IPHONE_CERT, "iPhone Developer");
   	}
	
}
