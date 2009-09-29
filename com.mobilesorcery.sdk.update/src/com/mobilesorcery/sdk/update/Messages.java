package com.mobilesorcery.sdk.update;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.mobilesorcery.sdk.update.messages"; //$NON-NLS-1$
	public static String UpdateManager_DownloadProgress;
	public static String UpdateManager_RestartingProgress;
	public static String UpdateManager_ServerBouncedRegReq;
	public static String UpdateManager_ServerBouncedRegReq1;
	public static String UpdateManager_ServerBouncedResendReq;
	public static String UpdateManager_UpdaterNotFoundError;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
