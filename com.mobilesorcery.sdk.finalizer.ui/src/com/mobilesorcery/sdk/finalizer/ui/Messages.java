package com.mobilesorcery.sdk.finalizer.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.mobilesorcery.sdk.finalizer.ui.messages"; //$NON-NLS-1$
	public static String FinalizeJob_Finalize;
	public static String FinalizeJob_NoProjectSelected;
	public static String FinalizerView_Finalize;
	public static String FinalizerView_FinalizerScript;
	public static String FinalizerView_GeneratingScript;
	public static String FinalizerView_NoProjectSelected;
	public static String FinalizerView_Propagate;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
