package com.mobilesorcery.sdk.deployment;

import java.util.HashMap;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.deployment.internal.ftp.FTPDeploymentStrategy;
import com.mobilesorcery.sdk.deployment.internal.ftp.FTPDeploymentStrategyFactory;

/**
 * The activator class controls the plug-in life cycle
 */
public class DeploymentPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.mobilesorcery.sdk.deployment";

	// The shared instance
	private static DeploymentPlugin plugin;

	private HashMap<String, IDeploymentStrategyFactory> factories = new HashMap<String, IDeploymentStrategyFactory>();
	
	/**
	 * The constructor
	 */
	public DeploymentPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		registerDeploymentStrategyFactory(FTPDeploymentStrategy.FACTORY_ID, new FTPDeploymentStrategyFactory());
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
	public static DeploymentPlugin getDefault() {
		return plugin;
	}

	public void registerDeploymentStrategyFactory(String id, IDeploymentStrategyFactory factory) {
		this.factories.put(id, factory);
	}
	
	public IDeploymentStrategyFactory getDeploymentStrategyFactory(String id) {
		return id == null ? null : factories.get(id);
	}
}
