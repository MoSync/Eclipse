package com.mobilesorcery.sdk.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.service.prefs.BackingStoreException;

public class PreferenceStorePropertyOwner extends PropertyOwnerBase {

	private final IPreferenceStore store;
	private final boolean useDefault;

	public PreferenceStorePropertyOwner(IPreferenceStore store) {
		this(store, false);
	}

	/**
	 * Creates a new {@link PreferenceStorePropertyOwner} with
	 * the option to work with the default values instead.
	 * @param store
	 * @param useDefault
	 */
	public PreferenceStorePropertyOwner(IPreferenceStore store, boolean useDefault) {
		this.store = store;
		this.useDefault = useDefault;
	}

	@Override
	public boolean setProperty(String key, String newValue) {
		String oldValue = getProperty(key);
		if (newValue == null) {
			store.setToDefault(key);
		} else {
			if (useDefault) {
				store.setDefault(key, newValue);
			} else {
				store.setValue(key, newValue);
			}
		}
		return !Util.equals(oldValue, newValue);
	}

	@Override
	public String getProperty(String key) {
		return useDefault ? store.getDefaultString(key) : store.getString(key);
	}

	@Override
	public String getDefaultProperty(String key) {
		return store.getDefaultString(key);
	}

	@Override
	public boolean isDefault(String key) {
		return useDefault || store.isDefault(key);
	}

	@Override
	public String getContext() {
		return null;
	}

	@Override
	public void initProperty(String key, String value) {
		setProperty(key, value);
	}

	@Override
	public Map<String, String> getProperties() {
		if (store instanceof ScopedPreferenceStore) {
			ScopedPreferenceStore scopedPrefs = (ScopedPreferenceStore) store;
			IEclipsePreferences[] nodes = scopedPrefs.getPreferenceNodes(useDefault);
			ArrayList<String> prefKeys = new ArrayList<String>();
			try {
				for (IEclipsePreferences node : nodes) {
					prefKeys.addAll(Arrays.asList(node.keys()));
				}
			} catch (BackingStoreException e) {
				CoreMoSyncPlugin.getDefault().log(e);
			}
			HashMap<String, String> result = new HashMap<String, String>();
			for (String key : prefKeys) {
				// TODO: Non-string prefs does not really work... but that's usually ok.
				String value = scopedPrefs.getString(key);
				if (!Util.isEmpty(value)) {
					result.put(key, value);
				}
			}
			return result;
		} else {
			throw new UnsupportedOperationException();
		}
	}

}
