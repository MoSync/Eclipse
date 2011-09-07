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
package com.mobilesorcery.sdk.builder.android;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.core.MoSyncProject;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "com.mobilesorcery.sdk.builder.android"; //$NON-NLS-1$

	public static final String EXTERNAL_SDK_PATH = "android.sdk";

	private static final String USE_FALLBACK = "use.fallback";

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

    public IPath getExternalAndroidSDKPath() {
    	String sdkPath = getPreferenceStore().getString(EXTERNAL_SDK_PATH);
    	return sdkPath == null ? null : new Path(sdkPath);
    }

	public static String getAndroidComponentName(MoSyncProject project) {
		String packageName = project.getProperty(PropertyInitializer.ANDROID_PACKAGE_NAME);
		String activityName = packageName + ".MoSync";
		String androidComponent = packageName + "/" + activityName;
		return androidComponent;
	}

	public void setUseFallback(boolean useFallback) {
		getPreferenceStore().setValue(USE_FALLBACK, useFallback);
	}

	public boolean shouldUseFallback() {
		return getPreferenceStore().getBoolean(USE_FALLBACK);
	}
}
