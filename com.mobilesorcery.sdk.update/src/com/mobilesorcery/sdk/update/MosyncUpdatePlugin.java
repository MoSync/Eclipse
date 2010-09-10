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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IUpdater;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.update.internal.RegistrationPartListener;

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

	public static Map<String, String> getDefaultParams(boolean hashOnly) {
	    HashMap<String, String> result = new HashMap<String, String>();
        IUpdater updater = CoreMoSyncPlugin.getDefault().getUpdater();
        // Whoa! We use the fact that we know our updater well...
        if (updater instanceof UpdateManagerBase) {
            result.putAll(((UpdateManagerBase) updater).assembleDefaultParams(hashOnly));
        }
        
        return result;
    }

}
