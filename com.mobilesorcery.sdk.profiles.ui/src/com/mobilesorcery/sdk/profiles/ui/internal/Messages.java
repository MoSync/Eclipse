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
package com.mobilesorcery.sdk.profiles.ui.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.mobilesorcery.sdk.profiles.ui.internal.messages"; //$NON-NLS-1$
	public static String ConstantFilterDialog_ConstantCriterion;
	public static String ConstantFilterDialog_Threshold;
	public static String DeviceFilterComposite_Add;
	public static String DeviceFilterComposite_Edit;
	public static String DeviceFilterComposite_NoFilter;
	public static String DeviceFilterComposite_Remove;
	public static String FeatureFilterDialog_Disallow;
	public static String FeatureFilterDialog_FeatureAltBug;
	public static String FeatureFilterDialog_Require;
	public static String ProfileFilterDialog_Disallow;
	public static String ProfileFilterDialog_Require;
	public static String ProfileFilterDialog_VendorAltDevice;
	public static String ProfilesView_CurrentProfile;
	public static String ProfilesView_InternalError;
	public static String ProfilesView_MoSyncToolConfigurationError;
	public static String ProfilesView_NoProfileSelected;
	public static String ProfilesView_NoUpdaterPlugin;
	public static String ProfilesView_ProfilesForProject;
	public static String ProfilesView_UpdateProfilesLink;
	public static String SelectFilterTypeDialog_SelectFilterType;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
