package com.mobilesorcery.sdk.builder.iphoneos;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.Util;

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

	public static final String IOS_BUNDLE_IDENTIFIER = PREFIX + "bundle.id";

	public static final String IOS_PROVISIONING_FILE = PREFIX + "provisioning.file";

	@Override
	public String getDefaultValue(IPropertyOwner p, String key) {
		if (IPHONE_PROJECT_SPECIFIC_CERT.equals(key)) {
			return PropertyUtil.fromBoolean(false);
		} else if (IOS_SDK_AUTO.equals(key) || IOS_SIM_SDK_AUTO.equals(key)){
			return PropertyUtil.fromBoolean(true);
		} else if (IOS_BUNDLE_IDENTIFIER.equals(key)) {
			return "com.%app-vendor%.%project-name%";
		} else if (IOS_PROVISIONING_FILE.equals(key)) { 
			return "";
		} else {
			return Activator.getDefault().getPreferenceStore().getString(key);
		}
	}

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		// The default cert; will also match user specific dev certs.
		store.setDefault(IPHONE_CERT, "iPhone Developer");
		// And we try to build as much as we can; but sometimes we can't!
		store.setDefault(Activator.ONLY_GENERATE_XCODE_PROJECT, !Util.isMac());
   	}

}
