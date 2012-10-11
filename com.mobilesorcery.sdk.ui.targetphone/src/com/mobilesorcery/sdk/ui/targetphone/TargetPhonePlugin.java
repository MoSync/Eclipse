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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.lib.JNALibInitializer;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.targetphone.internal.TargetPhoneTransportProxy;
import com.mobilesorcery.sdk.ui.targetphone.internal.bt.BTDIALOG;
import com.mobilesorcery.sdk.ui.targetphone.internal.bt.BTTargetPhoneTransport;

/**
 * The activator class controls the plug-in life cycle
 */
public class TargetPhonePlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.mobilesorcery.sdk.ui.targetphone";

	private static final String TARGET_PHONE_PREF_PREFIX = "target.phone.";

	private static final String TARGET_PHONE_PREF_NAME_SUFFIX = ".name";

	private static final String TARGET_PHONE_PREF_ADDR_SUFFIX = ".addr";

	private static final String TARGET_PHONE_PREF_PORT_SUFFIX = ".port";

	private static final String TARGET_PHONE_PREF_SELECTED_SUFFIX = ".current";

	private static final String TARGET_PHONE_PREF_PROFILE_SUFFIX = ".preferred.profile";

	private static final int MAX_HISTORY_SIZE = 15;

	public static final String DEFAULT_TARGET_PHONE_TRANSPORT = "bt";

	// The shared instance
	private static TargetPhonePlugin plugin;

	private final LinkedList<ITargetPhone> history = new LinkedList<ITargetPhone>();

	private ITargetPhone selectedPhone;

	private final HashMap<String, ITargetPhoneTransport> transports = new HashMap<String, ITargetPhoneTransport>();

	private final CopyOnWriteArrayList<ITargetPhoneTransportListener> listeners = new CopyOnWriteArrayList<ITargetPhoneTransportListener>();

	/**
	 * The constructor
	 */
	public TargetPhonePlugin() {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		initNativeLibs(context);
		initTargetPhoneTransports();
		initTargetPhoneHistory();
	}

	private void initTargetPhoneHistory() {
		Reader historyInput = null;
		try {
			File historyInputFile = getHistoryOutputPath().toFile();
			if (!historyInputFile.exists()) {
				return;
			}

			historyInput = new FileReader(historyInputFile);
			XMLMemento historyMemento = XMLMemento.createReadRoot(historyInput);
			IMemento[] devices = historyMemento.getChildren("device");

			history.clear();
			Integer selected = historyMemento.getInteger("selected");

			for (int i = 0; i < devices.length; i++) {
				IMemento device = devices[i];
				String transportId = device.getString("transport");
				ITargetPhoneTransport transport = getTargetPhoneTransport(transportId);
				if (transport != null) {
					Integer ix = device.getInteger("ix");
					String name = device
							.getString(ITargetPhoneTransport.NAME_ATTR);
					String legacyProfile = device
							.getString(ITargetPhoneTransport.LEGACY_PROFILE_ATTR);
					String platformProfile = device
							.getString(ITargetPhoneTransport.DEFAULT_PROFILE_ATTR);
					ITargetPhone targetPhone = transport.load(device, name);
					targetPhone.setPreferredProfile(MoSyncTool.LEGACY_PROFILE_TYPE, MoSyncTool.getDefault().getProfileManager(MoSyncTool.LEGACY_PROFILE_TYPE)
							.getProfile(legacyProfile));
					targetPhone.setPreferredProfile(MoSyncTool.DEFAULT_PROFILE_TYPE, MoSyncTool.getDefault().getProfileManager(MoSyncTool.DEFAULT_PROFILE_TYPE)
							.getProfile(platformProfile));
					history.addFirst(targetPhone);
					if (Util.equals(ix, selected)) {
						selectedPhone = targetPhone;
					}
				}
			}
		} catch (Exception e) {
			CoreMoSyncPlugin.getDefault().log(e);
		} finally {
			Util.safeClose(historyInput);
		}
	}

	private void initTargetPhoneTransports() {
		// The default (BT) transport
		addTransport(new BTTargetPhoneTransport());

		// Other (extension points) transports
		IConfigurationElement[] transportExtensions = Platform.getExtensionRegistry().getConfigurationElementsFor("com.mobilesorcery.sdk.targetphonetransports");
		for (int i = 0; i < transportExtensions.length; i++) {
			IConfigurationElement transportExtension = transportExtensions[i];
			String transportId = transportExtension.getAttribute("id");
			if (transportId != null) {
				addTransport(new TargetPhoneTransportProxy(transportExtension));
			}

		}
	}

	private void addTransport(ITargetPhoneTransport transport) {
		transports.put(transport.getId(), transport);
		getImageRegistry().put(transport.getId(), transport.getIcon());
	}

	public ITargetPhoneTransport getTargetPhoneTransport(String transportId) {
		return transports.get(transportId);
	}

	public List<ITargetPhoneTransport> getTargetPhoneTransports() {
		ArrayList<ITargetPhoneTransport> result = new ArrayList<ITargetPhoneTransport>();
		for (ITargetPhoneTransport transport : transports.values()) {
			if (transport.isAvailable()) {
				result.add(transport);
			}
		}
		return result;
	}

	private void storeTargetPhoneHistory() throws CoreException {
		int i = 0;
		XMLMemento historyMemento = XMLMemento.createWriteRoot("history");

		for (Iterator<ITargetPhone> phones = this.history.iterator(); phones
				.hasNext();) {
			ITargetPhone phone = phones.next();

			ITargetPhoneTransport transport = phone.getTransport();
			if (transport != null) {
				IMemento deviceMemento = historyMemento.createChild("device");
				deviceMemento.putInteger("ix", i);
				deviceMemento.putString("transport", transport.getId());
				deviceMemento.putString(ITargetPhoneTransport.NAME_ATTR, phone
						.getName());
				deviceMemento.putString(ITargetPhoneTransport.LEGACY_PROFILE_ATTR,
						MoSyncTool.toString(phone.getPreferredProfile(MoSyncTool.LEGACY_PROFILE_TYPE)));
				deviceMemento.putString(ITargetPhoneTransport.DEFAULT_PROFILE_ATTR,
						MoSyncTool.toString(phone.getPreferredProfile(MoSyncTool.DEFAULT_PROFILE_TYPE)));
				transport.store(phone, deviceMemento);
				if (selectedPhone == phone) {
					historyMemento.putInteger("selected", i);
				}
			}

			i++;
		}

		IPath historyOutputPath = getHistoryOutputPath();
		Writer historyOutputWriter = null;
		try {
			historyOutputWriter = new FileWriter(historyOutputPath.toFile());
			historyMemento.save(historyOutputWriter);
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, e
					.getMessage(), e));
		} finally {
			Util.safeClose(historyOutputWriter);
		}
	}

	private IPath getHistoryOutputPath() {
		return Platform.getStateLocation(getBundle()).append("history.xml");
	}

	private void initNativeLibs(BundleContext context) {
		if(System.getProperty("os.name").toLowerCase().indexOf("win") == -1)
		{
			return;
		}

		try {
			JNALibInitializer.init(getBundle(), "libbtdialog");
			BTDIALOG dummy = BTDIALOG.INSTANCE; // Just to execute the .clinit.
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		try {
			storeTargetPhoneHistory();
		} finally {
			plugin = null;
			super.stop(context);
		}
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static TargetPhonePlugin getDefault() {
		return plugin;
	}

	public List<ITargetPhone> getSelectedTargetPhoneHistory() {
		return history;
	}

	public void addToHistory(ITargetPhone phone) {
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

	public void setCurrentlySelectedPhone(ITargetPhone selectedPhone) {
		this.selectedPhone = selectedPhone;
	}

	public ITargetPhone getCurrentlySelectedPhone() {
		return selectedPhone;
	}

	public void clearHistory() {
		history.clear();
		selectedPhone = null;
	}

	public int getCurrentProfileManagerType() {
		MoSyncProject current = MosyncUIPlugin.getDefault().getCurrentlySelectedProject(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		if (current == null) {
			return MoSyncTool.DEFAULT_PROFILE_TYPE;
		}
		return current.getProfileManagerType();
	}

	public void addTargetPhoneTransportListener(ITargetPhoneTransportListener listener) {
		listeners.add(listener);
	}

	public void removeTargetPhoneTransportListener(ITargetPhoneTransportListener listener) {
		listeners.remove(listener);
	}

	public void notifyListeners(TargetPhoneTransportEvent event) {
		for (ITargetPhoneTransportListener listener : listeners) {
			listener.handleEvent(event);
		}
	}
}
