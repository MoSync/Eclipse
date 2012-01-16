package com.mobilesorcery.sdk.builder.winmobilecs;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class WinMobileCSPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.mobilesorcery.sdk.builder.winmobilecs"; //$NON-NLS-1$

	public static final String WP_EMULATOR_SPECIFIER = "emulator";

	// The shared instance
	private static WinMobileCSPlugin plugin;

	/**
	 * The constructor
	 */
	public WinMobileCSPlugin() {
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
	public static WinMobileCSPlugin getDefault() {
		return plugin;
	}

}
