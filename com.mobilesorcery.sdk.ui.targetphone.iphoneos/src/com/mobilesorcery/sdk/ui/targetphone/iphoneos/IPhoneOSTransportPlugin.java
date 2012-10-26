package com.mobilesorcery.sdk.ui.targetphone.iphoneos;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class IPhoneOSTransportPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.mobilesorcery.sdk.ui.targetphone.iphoneos"; //$NON-NLS-1$

	public static final String PROV_IMAGE = "prov";

	// The shared instance
	private static IPhoneOSTransportPlugin plugin;
	
	/**
	 * The constructor
	 */
	public IPhoneOSTransportPlugin() {
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
	
	public void initializeImageRegistry(ImageRegistry reg) {
		reg.put(PROV_IMAGE, imageDescriptorFromPlugin(PLUGIN_ID, "$nl$/icons/prov.gif"));
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static IPhoneOSTransportPlugin getDefault() {
		return plugin;
	}

	public URL getServerURL() throws IOException {
		InetAddress localHost = InetAddress.getLocalHost();
		String host = localHost.getHostAddress();
		return new URL("http", host, 34004, "");
	}

}
