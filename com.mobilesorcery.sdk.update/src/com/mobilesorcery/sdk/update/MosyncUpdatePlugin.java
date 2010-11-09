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
package com.mobilesorcery.sdk.update;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IUpdater;
import com.mobilesorcery.sdk.update.internal.DefaultUpdater2;

/**
 * The activator class controls the plug-in life cycle
 */
public class MosyncUpdatePlugin extends AbstractUIPlugin {

    // The plug-in ID
	public static final String PLUGIN_ID = "com.mobilesorcery.sdk.update"; //$NON-NLS-1$

	// The shared instance
	private static MosyncUpdatePlugin plugin;

	/**
	 * The constructor
	 */
	public MosyncUpdatePlugin() {
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
	public static MosyncUpdatePlugin getDefault() {
		return plugin;
	}
	
	public void tryToCloseRegistrationPerspective() {
		IUpdater updater = CoreMoSyncPlugin.getDefault().getUpdater();
		if (updater instanceof DefaultUpdater2) {
			((DefaultUpdater2) updater).closeRegistrationPerspective();
		}
	}

}
