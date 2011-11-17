package com.mobilesorcery.sdk.capabilities.devices;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.capabilities.devices.internal.DeviceCapabilitiesParser;
import com.mobilesorcery.sdk.core.ICapabilities;
import com.mobilesorcery.sdk.profiles.IProfile;

/**
 * The activator class controls the plug-in life cycle
 */
public class DeviceCapabilitesPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.mobilesorcery.sdk.capabilities.devices"; //$NON-NLS-1$

	// The shared instance
	private static DeviceCapabilitesPlugin plugin;

	private DeviceCapabilitiesParser parser;
	
	/**
	 * The constructor
	 */
	public DeviceCapabilitesPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		initializeDefaultCapabilitiesMaps();
	}

	private void initializeDefaultCapabilitiesMaps() {
		parser = new DeviceCapabilitiesParser();
	}
	
	public ICapabilities getCapabilitiesForProfile(IProfile profile) {
		return parser.getCapabilitiesForProfile(profile);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static DeviceCapabilitesPlugin getDefault() {
		return plugin;
	}

}
