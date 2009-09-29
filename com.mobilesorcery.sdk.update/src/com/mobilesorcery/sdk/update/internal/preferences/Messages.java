package com.mobilesorcery.sdk.update.internal.preferences;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.mobilesorcery.sdk.update.internal.preferences.messages"; //$NON-NLS-1$
	public static String ProfileUpdatesPreferencePage_AreYouSureMessage;
	public static String ProfileUpdatesPreferencePage_AreYouSureTitle;
	public static String ProfileUpdatesPreferencePage_AutoUpdate;
	public static String ProfileUpdatesPreferencePage_ClearRegistrationInfo;
	public static String ProfileUpdatesPreferencePage_NotRegistered;
	public static String ProfileUpdatesPreferencePage_RegistrationDescription;
	public static String ProfileUpdatesPreferencePage_Title;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
