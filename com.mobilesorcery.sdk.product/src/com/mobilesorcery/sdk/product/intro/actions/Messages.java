package com.mobilesorcery.sdk.product.intro.actions;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.mobilesorcery.sdk.product.intro.actions.messages"; //$NON-NLS-1$
	public static String ImportExampleAction_ImportError;
	public static String ImportExampleAction_NoProjectSpecified;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
