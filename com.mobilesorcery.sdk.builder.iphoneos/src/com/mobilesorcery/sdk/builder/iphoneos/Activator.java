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
package com.mobilesorcery.sdk.builder.iphoneos;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PropertyUtil;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "com.mobilesorcery.sdk.builder.iphoneose"; //$NON-NLS-1$

    public final static String IOS_SIMULATOR_SPECIFIER = "simulator";

    // The shared instance
    private static Activator plugin;

    /**
     * The constructor
     */
    public Activator() {
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    @Override
	public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
	public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

    /**
     * Returns the current SDKs for a project
     * @param project
     * @param sdkType
     * @return
     */
	public SDK getSDK(MoSyncProject project, int sdkType) {
		boolean useDefault = PropertyUtil.getBoolean(project, sdkType == XCodeBuild.IOS_SDKS ? PropertyInitializer.IOS_SDK_AUTO : PropertyInitializer.IOS_SIM_SDK_AUTO);
		String sdkId = project.getProperty(sdkType == XCodeBuild.IOS_SDKS ? PropertyInitializer.IOS_SDK : PropertyInitializer.IOS_SIM_SDK);
		SDK sdk = useDefault ? XCodeBuild.getDefault().getDefaultSDK(sdkType) : XCodeBuild.getDefault().getSDK(sdkId);
		return sdk;
	}

	/**
	 * Set the SDK to use for a project
	 * @param project
	 * @param sdkType
	 * @param sdk
	 * @param useDefault
	 */
	public void setSDK(MoSyncProject project, int sdkType, SDK sdk, boolean useDefault) {
		String sdkId = useDefault || sdk == null ? null : sdk.getId();
		PropertyUtil.setBoolean(project, sdkType == XCodeBuild.IOS_SDKS ? PropertyInitializer.IOS_SDK_AUTO : PropertyInitializer.IOS_SIM_SDK_AUTO, useDefault);
		project.setProperty(sdkType == XCodeBuild.IOS_SDKS ? PropertyInitializer.IOS_SDK : PropertyInitializer.IOS_SIM_SDK, sdkId);
	}


}
