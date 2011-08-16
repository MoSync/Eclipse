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

    public final static String IOS_SDK = PREFIX + "ios.sdk";
    
    public final static String IOS_SDK_AUTO = IOS_SDK + ".auto";
    
    public final static String IOS_SIM_SDK = IOS_SDK + ".simulator";
    
    public final static String IOS_SIM_SDK_AUTO = IOS_SIM_SDK + ".auto";
    
	@Override
	public String getDefaultValue(IPropertyOwner p, String key) {
		if (IPHONE_PROJECT_SPECIFIC_CERT.equals(key)) {
			return PropertyUtil.fromBoolean(false);
		} else if (IOS_SDK_AUTO.equals(key) || IOS_SIM_SDK_AUTO.equals(key)){
			return PropertyUtil.fromBoolean(true);
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
