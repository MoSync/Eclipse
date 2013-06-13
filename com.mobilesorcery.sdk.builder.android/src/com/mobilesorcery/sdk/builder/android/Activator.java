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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Version;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhoneTransportListener;
import com.mobilesorcery.sdk.ui.targetphone.TargetPhonePlugin;
import com.mobilesorcery.sdk.ui.targetphone.TargetPhoneTransportEvent;
import com.mobilesorcery.sdk.ui.targetphone.android.AndroidTargetPhone;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin implements ITargetPhoneTransportListener {

    // The plug-in ID
    public static final String PLUGIN_ID = "com.mobilesorcery.sdk.builder.android"; //$NON-NLS-1$

    public static final String EXTERNAL_SDK_PATH = "android.sdk";
    
    public static final String NDK_PATH = "android.ndk";
    
    public static final String NDK_PLATFORM_VERSION = "android.platform.version";

    // The shared instance
    private static Activator plugin;

	private List<NdkToolchain> ndkToolchains;

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
        TargetPhonePlugin.getDefault().addTargetPhoneTransportListener(this);
        plugin = this;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
	public void stop(BundleContext context) throws Exception {
        plugin = null;
        TargetPhonePlugin.getDefault().removeTargetPhoneTransportListener(this);
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

	@Override
	public void handleEvent(TargetPhoneTransportEvent event) {
		// TODO: This should be centralized!
		if (event.phone instanceof AndroidTargetPhone) {
			if (event.project.getActiveBuildConfiguration().isType(IBuildConfiguration.DEBUG_TYPE)) {
				
			}
		}
	}

	public NdkToolchain getPreferredNdkToolchain() {
		List<NdkToolchain> toolchains = getNdkToolchains();
		// TODO: These could be preferences!
		TreeMap<Integer, NdkToolchain> toolchainScores = new TreeMap<Integer, NdkToolchain>();
		for (NdkToolchain toolchain : toolchains) {
			int toolchainScore = 0;
			if (toolchain.isCompatible()) {
				if (toolchain.getVersion().equals(new Version("4.6"))) {
					toolchainScore += 2;
				}
				if (toolchain.getVersion().equals(new Version("4.7"))) {
					toolchainScore += 1;
				}
			}
			toolchainScores.put(toolchainScore, toolchain);
		}
		return toolchainScores.isEmpty() ? null : toolchainScores.lastEntry().getValue();
	}
	
	public synchronized List<NdkToolchain> getNdkToolchains() {
		if (ndkToolchains == null) {
			ArrayList<NdkToolchain> ndkToolchainsWrite = new ArrayList<NdkToolchain>();
			String ndkLocation = getPreferenceStore().getString(Activator.NDK_PATH);
			File toolchainDir = new File(ndkLocation, "toolchains");
			String[] toolchains = toolchainDir.list();
			for (String toolchain : toolchains) {
				int ixVersion = toolchain.lastIndexOf("-");
				if (ixVersion != -1) {
					String versionStr = toolchain.substring(ixVersion + 1);
					Version version = new Version(versionStr);
					String arch = toolchain.substring(0, ixVersion);
					ndkToolchainsWrite.add(new NdkToolchain(new Path(toolchainDir.getAbsolutePath()).append(toolchain), arch, version));
				}
			}
			ndkToolchains = Collections.unmodifiableList(ndkToolchainsWrite);
		}
		return ndkToolchains;
	}
}
