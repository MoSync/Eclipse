package com.mobilesorcery.sdk.core.templates;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.mobilesorcery.sdk.core.templates.messages"; //$NON-NLS-1$
	public static String ProjectTemplate_InvalidTemplate_0;
	public static String ProjectTemplate_InvalidTemplateRoot;
	public static String ProjectTemplate_ProjectInitFailed_0;
	public static String ProjectTemplate_ProjectInitFailed_1;
	public static String ProjectTemplate_ProjectInitFailed_2;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
