package com.mobilesorcery.sdk.product.intro;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.mobilesorcery.sdk.product.intro.messages"; //$NON-NLS-1$
	public static String ImportExampleProject_0;
	public static String ImportExampleProject_ExampleNotFound;
	public static String ImportExampleProject_ExampleNotFoundMessage;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
