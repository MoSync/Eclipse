package com.mobilesorcery.sdk.builder.s60;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.mobilesorcery.sdk.builder.s60.messages"; //$NON-NLS-1$
	public static String DefaultKeyInitializer_ConsoleName;
	public static String DefaultKeyInitializer_InitializingDefaultKeys;
	public static String S60Packager_ApplicationNameTooLong;
	public static String S60Packager_InvalidTemplate;
	public static String V2Packager_PackageError;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
