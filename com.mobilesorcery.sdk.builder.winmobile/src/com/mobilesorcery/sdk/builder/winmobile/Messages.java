package com.mobilesorcery.sdk.builder.winmobile;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.mobilesorcery.sdk.builder.winmobile.messages"; //$NON-NLS-1$
	public static String WinMobilePackager_PackageError;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
