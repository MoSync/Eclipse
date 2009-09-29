package com.mobilesorcery.sdk.profiles.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.mobilesorcery.sdk.profiles.ui.messages"; //$NON-NLS-1$
	public static String ProfileLabelProvider_UnknownObject;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
