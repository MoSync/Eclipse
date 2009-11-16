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
package com.mobilesorcery.sdk.ui.targetphone;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.Path;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.lib.JNALibInitializer;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.ui.targetphone.internal.BTDIALOG;
import com.mobilesorcery.sdk.ui.targetphone.internal.TargetPhone;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.mobilesorcery.sdk.ui.targetphone";
	
    private static final String TARGET_PHONE_PREF_PREFIX = "target.phone.";

    private static final String TARGET_PHONE_PREF_NAME_SUFFIX = ".name";
    
    private static final String TARGET_PHONE_PREF_ADDR_SUFFIX = ".addr";

    private static final String TARGET_PHONE_PREF_PORT_SUFFIX = ".port";

    private static final String TARGET_PHONE_PREF_SELECTED_SUFFIX = ".current";

	private static final String TARGET_PHONE_PREF_PROFILE_SUFFIX = ".preferred.profile";

    private static final int MAX_HISTORY_SIZE = 5;

	// The shared instance
	private static Activator plugin;

    private LinkedList<TargetPhone> history = new LinkedList<TargetPhone>();

    private TargetPhone selectedPhone;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		initNativeLibs(context);
		initTargetPhoneHistory();
		plugin = this;
	}

	private void initTargetPhoneHistory() {
	    getPreferenceStore().setDefault(TARGET_PHONE_PREF_PREFIX  + TARGET_PHONE_PREF_SELECTED_SUFFIX, -1);
	    int currentlySelected = getPreferenceStore().getInt(TARGET_PHONE_PREF_PREFIX  + TARGET_PHONE_PREF_SELECTED_SUFFIX);
	    
	    for (int i = 0; i < MAX_HISTORY_SIZE; i++) {
            String name = getPreferenceStore().getString(TARGET_PHONE_PREF_PREFIX + i + TARGET_PHONE_PREF_NAME_SUFFIX);
            String addr = getPreferenceStore().getString(TARGET_PHONE_PREF_PREFIX + i + TARGET_PHONE_PREF_ADDR_SUFFIX);
            int port = getPreferenceStore().getInt(TARGET_PHONE_PREF_PREFIX + i + TARGET_PHONE_PREF_PORT_SUFFIX);
            if (port <= 0) {
                port = TargetPhone.PORT_UNASSIGNED;
            }
            
            String profileName = getPreferenceStore().getString(TARGET_PHONE_PREF_PREFIX + i + TARGET_PHONE_PREF_PROFILE_SUFFIX);
            IProfile preferredProfile = MoSyncTool.getDefault().getProfile(profileName);
            
            if (name != null && addr != null && !"".equals(name) && !"".equals(addr)) {
                TargetPhone addedPhone = new TargetPhone(name.toCharArray(), Util.fromBase16(addr), port);
                addedPhone.setPreferredProfile(preferredProfile);
                history.addFirst(addedPhone);
                if (i == currentlySelected) {
                    selectedPhone = addedPhone; 
                }
            }            
	    }
	    
    }
	
	private void storeTargetPhoneHistory() {
	    int i = 0;
	    for (Iterator<TargetPhone> phones = this.history.iterator(); phones.hasNext(); ) {
	        TargetPhone phone = phones.next();
	        String name = phone.getName();
	        String addr = Util.toBase16(phone.getAddressAsBytes());
	        int port = phone.getPort();
	        
            getPreferenceStore().setValue(TARGET_PHONE_PREF_PREFIX + i + TARGET_PHONE_PREF_NAME_SUFFIX, name);
            getPreferenceStore().setValue(TARGET_PHONE_PREF_PREFIX + i + TARGET_PHONE_PREF_ADDR_SUFFIX, addr);
            getPreferenceStore().setValue(TARGET_PHONE_PREF_PREFIX + i + TARGET_PHONE_PREF_PORT_SUFFIX, port);
            getPreferenceStore().setValue(TARGET_PHONE_PREF_PREFIX + i + TARGET_PHONE_PREF_PROFILE_SUFFIX, MoSyncTool.toString(phone.getPreferredProfile()));
            
            if (selectedPhone == phone) {
                getPreferenceStore().setValue(TARGET_PHONE_PREF_PREFIX + TARGET_PHONE_PREF_SELECTED_SUFFIX, i);
            }
            
	        i++;
	    }
	}

    private void initNativeLibs(BundleContext context) {
	    try {
	        JNALibInitializer.init(getBundle(), new Path("btDialog.dll"));
	        BTDIALOG dummy = BTDIALOG.INSTANCE; // Just to execute the .clinit.
	    } catch (Throwable t) {
	        t.printStackTrace();
	    }
    }

    /*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		try {
		    storeTargetPhoneHistory();
		} finally {
		    super.stop(context);
		}
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	public List<TargetPhone> getSelectedTargetPhoneHistory() {
	    return history;
	}
	
	public void addToHistory(TargetPhone phone) {
	    if (phone == null) {
	        return;
	    }
	    history.remove(phone);
	    history.addFirst(phone);
	    setCurrentlySelectedPhone(phone);
	    while (history.size() > MAX_HISTORY_SIZE) {
	        history.removeLast();
	    }
	}
	
	public void setCurrentlySelectedPhone(TargetPhone selectedPhone) {
	    this.selectedPhone = selectedPhone;
	}
	
	public TargetPhone getCurrentlySelectedPhone() {
	    return selectedPhone;
	}

	public void clearHistory() {
		history.clear();
	}
}
