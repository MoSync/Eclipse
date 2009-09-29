package com.mobilesorcery.sdk.builder.app;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.mobilesorcery.sdk.builder.app.messages"; //$NON-NLS-1$
	public static String HeadlessBuild_IllegalProjectState;
	public static String HeadlessBuild_Usage;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
