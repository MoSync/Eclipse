/*  Copyright (C) 2011 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.html5.ui;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.json.simple.JSONObject;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;
import com.mobilesorcery.sdk.html5.live.ILiveServerListener;

/**
 * The activator class controls the plug-in life cycle
 */
public class Html5UiPlugin extends AbstractUIPlugin implements ILiveServerListener {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.mobilesorcery.sdk.html5.ui"; //$NON-NLS-1$

	// The shared instance
	private static Html5UiPlugin plugin;

	/**
	 * The constructor
	 */
	public Html5UiPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		Html5Plugin.getDefault().getReloadServer().addListener(this);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
		Html5Plugin.getDefault().getReloadServer().removeListener(this);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Html5UiPlugin getDefault() {
		return plugin;
	}

	@Override
	public void received(String command, JSONObject json) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void timeout(ReloadVirtualMachine vm) {
		
	}

}
