package com.mobilesorcery.sdk.finalizer.core;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.mobilesorcery.sdk.finalizer.core.messages"; //$NON-NLS-1$
	public static String FinalizerParser_BuildFailed;
	public static String FinalizerParser_FinalizingProgress;
	public static String FinalizerParser_ParseError_0;
	public static String FinalizerParser_ParseError_3;
	public static String FinalizerParser_ParseError_UnknownProfile;
	public static String FinalizerParser_ParseError_UnknownVender;
	public static String FinalizerParser_ScriptBoilerplate;
	public static String FinalizerParser_ScriptNoProfiles;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
