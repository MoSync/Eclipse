package com.mobilesorcery.sdk.profiles.ui.internal.actions;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.mobilesorcery.sdk.profiles.ui.internal.actions.messages"; //$NON-NLS-1$
	public static String SetTargetProfileAction_SetTargetPhone;
	public static String ShowProfileInfoAction_ProfilerViewerError;
	public static String ShowProfileInfoAction_ShowProfileInfo;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
