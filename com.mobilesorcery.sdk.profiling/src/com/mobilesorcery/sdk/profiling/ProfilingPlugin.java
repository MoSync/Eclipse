/*  Copyright (C) 2010 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.profiling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.profiling.IProfilingListener.ProfilingEventType;
import com.mobilesorcery.sdk.profiling.internal.ShowProfilingViewListener;

/**
 * The activator class controls the plug-in life cycle
 */
public class ProfilingPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.mobilesorcery.sdk.profiling";

	// The shared instance
	private static ProfilingPlugin plugin;
	
	private CopyOnWriteArrayList<IProfilingListener> listeners = new CopyOnWriteArrayList<IProfilingListener>();

    private ArrayList<IProfilingSession> sessions = new ArrayList<IProfilingSession>();
	
	/**
	 * The constructor
	 */
	public ProfilingPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		addProfilingListener(ShowProfilingViewListener.getInstance());
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
		removeProfilingListener(ShowProfilingViewListener.getInstance());
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static ProfilingPlugin getDefault() {
		return plugin;
	}
	
	public void addProfilingListener(IProfilingListener listener) {
        listeners.add(listener);
    }
	
	public void removeProfilingListener(IProfilingListener listener) {
        listeners.remove(listener);
    }
	
	public void notifyProfilingListeners(ProfilingEventType profilingEventType, IProfilingSession session) {
	    if (profilingEventType == ProfilingEventType.STARTED) {
	        sessions.add(session);
	    } else if (profilingEventType == ProfilingEventType.REMOVED) {
	        sessions.remove(session);
	    }
	    
	    for (IProfilingListener listener : listeners) {
	        listener.handleEvent(profilingEventType, session);
	    }
	}
	
	public List<IProfilingSession> getSessions() {
	    return Collections.unmodifiableList(sessions);
	}

    public IFilter<IInvocation> getDefaultFilter() {
        return new IFilter<IInvocation>() {
            public boolean accept(IInvocation invocation) {
                return true;
            }
        };
    }

}
