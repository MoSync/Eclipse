package com.mobilesorcery.sdk.capabilities.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.capabilities.core.apianalysis.APICapabilitiesAnalyzer;
import com.mobilesorcery.sdk.capabilities.core.internal.DefaultCapabilitiesMatcher;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;

/**
 * The activator class controls the plug-in life cycle
 */
public class CapabilitiesPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.mobilesorcery.sdk.capabilities.ui"; //$NON-NLS-1$

	// The shared instance
	private static CapabilitiesPlugin plugin;

	private DefaultCapabilitiesMatcher defaultMatcher;
	
	/**
	 * The constructor
	 */
	public CapabilitiesPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		initCapabilitiesMatcher();
	}

	private void initCapabilitiesMatcher() {
		defaultMatcher = new DefaultCapabilitiesMatcher();
		IConfigurationElement[] matcherExtensions = Platform.getExtensionRegistry().getConfigurationElementsFor("com.mobilesorcery.sdk.capabilities.matchers");
		for (int i = 0; i < matcherExtensions.length; i++) {
			try {
				ICapabilitiesMatcher matcher = (ICapabilitiesMatcher) matcherExtensions[i].createExecutableExtension("class");
				// TODO: Add proxy!
				defaultMatcher.add(matcher);
			} catch (Exception e) {
				CoreMoSyncPlugin.getDefault().log(e);
			}
		}
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
	public static CapabilitiesPlugin getDefault() {
		return plugin;
	}

	public ICapabilitiesAnalyzer createAnalyzer() {
		return new APICapabilitiesAnalyzer();
	}
	
	public ICapabilitiesMatcher getMatcher() {
		return defaultMatcher;
	}
}
