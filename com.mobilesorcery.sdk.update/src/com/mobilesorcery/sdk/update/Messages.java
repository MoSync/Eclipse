/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
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
