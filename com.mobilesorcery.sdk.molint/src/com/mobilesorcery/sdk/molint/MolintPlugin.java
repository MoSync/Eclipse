package com.mobilesorcery.sdk.molint;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.molint.rules.MAHeaderRule;

/**
 * The activator class controls the plug-in life cycle
 */
public class MolintPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.mobilesorcery.sdk.molint"; //$NON-NLS-1$

	// The shared instance
	private static MolintPlugin plugin;

	private ArrayList<IMolintRule> rules;
	
	/**
	 * The constructor
	 */
	public MolintPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
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
	public static MolintPlugin getDefault() {
		return plugin;
	}

	public boolean isMolintEnabled() {
		return getPreferenceStore().getBoolean("enabled");
	}
	
	public void setMolintEnabled(boolean enbled) {
		getPreferenceStore().setValue("enabled", enbled);
	}
	
	public synchronized List<IMolintRule> getAllRules() {
		if (rules == null) {
			// TODO: Extensions?
			rules = new ArrayList<IMolintRule>();
			rules.add(new MAHeaderRule());
		}
		return rules;
	}

}
