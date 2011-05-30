package com.mobilesorcery.sdk.core;

import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;

public class PreferenceStorePropertyOwner extends PropertyOwnerBase {

	private IPreferenceStore store;
	private boolean useDefault;

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
		throw new UnsupportedOperationException();
	}

}
