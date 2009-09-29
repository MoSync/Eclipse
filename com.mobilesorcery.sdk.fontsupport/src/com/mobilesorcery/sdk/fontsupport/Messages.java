package com.mobilesorcery.sdk.fontsupport;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.mobilesorcery.sdk.fontsupport.messages"; //$NON-NLS-1$
	public static String MOF_FontImageMissing;
	public static String MOF_InvalidImageFormat;
	public static String MOF_NoImageFile;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
