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

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.ui.DefaultMessageProvider;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "com.mobilesorcery.sdk.builder.iphoneose"; //$NON-NLS-1$

    public final static String IOS_SIMULATOR_SPECIFIER = "simulator";

    public final static String ONLY_GENERATE_XCODE_PROJECT = PLUGIN_ID + ".build.xcode";
    
    public final static String IPHONE_DEV_CERT = "iPhone Developer";
    
    public final static String IPHONE_DIST_CERT = "iPhone Distribution";

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

	/**
	 * Validates a UTI, which is used for bundler identifiers etc.
	 * @param ignoreParameters 
	 * @param uti
	 * @return
	 */
	public IMessageProvider validateBundleIdentifier(String bundleId, boolean ignoreParameters) {
    	String message = null;
    	int messageType = IMessageProvider.ERROR;
    	if (Util.isEmpty(bundleId)) {
    		message = "Bundle identifier cannot be empty";
    	} else if (bundleId.contains("..")) {
    		message = MessageFormat.format("Bundle identifier cannot have empty segment ({0})", bundleId);
    	} else if (bundleId.endsWith(".") || bundleId.startsWith(".")) {
    		message = MessageFormat.format("Bundle identifier cannot start or end with '.' ({0})", bundleId);
    	} else if (containsInvalidChars(bundleId, ignoreParameters)) {
    		message = MessageFormat.format("Bundle identifier segments can only contain letters, digits and '-'. ({0})", bundleId);
    	} else if (!bundleId.startsWith("com.")) {
			message = "Bundle identifier should start with 'com.'";
			messageType = IMessageProvider.WARNING;
		} else if (bundleId.split("\\.").length != 3) {
			message = "Recommended bundle identifier format: com.YOURCOMPANY.YOURAPP";
			messageType = IMessageProvider.WARNING;
		}
		
		return Util.isEmpty(message) ? DefaultMessageProvider.EMPTY : new DefaultMessageProvider(message, messageType);
	}

    private boolean containsInvalidChars(String bundleId, boolean ignoreParameters) {
		for (int i = 0; i < bundleId.length(); i++) {
			char ch = bundleId.charAt(i);
			boolean valid = ch > 32 && ch < 128 && (Character.isLetter(ch) || Character.isDigit(ch) || ch == '-' || ch == '.');
			valid |= (ignoreParameters && ch == '%');
			if (!valid) {
				return true;
			}
		}
		return false;
	}
}
