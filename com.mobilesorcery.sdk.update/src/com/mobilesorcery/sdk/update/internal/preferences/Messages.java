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
package com.mobilesorcery.sdk.update.internal.preferences;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.mobilesorcery.sdk.update.internal.preferences.messages"; //$NON-NLS-1$
	public static String ProfileUpdatesPreferencePage_AreYouSureMessage;
	public static String ProfileUpdatesPreferencePage_AreYouSureTitle;
	public static String ProfileUpdatesPreferencePage_AutoUpdate;
	public static String ProfileUpdatesPreferencePage_ClearRegistrationInfo;
	public static String ProfileUpdatesPreferencePage_NotRegistered;
	public static String ProfileUpdatesPreferencePage_RegistrationDescription;
	public static String ProfileUpdatesPreferencePage_Title;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
