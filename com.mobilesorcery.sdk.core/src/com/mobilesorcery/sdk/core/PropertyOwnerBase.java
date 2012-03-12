package com.mobilesorcery.sdk.core;

import java.util.Map;

/**
 * A simple base class for {@link IPropertyOwner}s.
 * @author Mattias Bybro
 *
 */
public abstract class PropertyOwnerBase implements IPropertyOwner {

	@Override
	public boolean applyProperties(Map<String, String> properties) {
		boolean result = false;
		for (String key : properties.keySet()) {
			String value = properties.get(key);
			result |= setProperty(key, value);
		}

		return result;
	}


	@Override
	public IWorkingCopy createWorkingCopy() {
		return new PropertyOwnerWorkingCopy(this);
	}

	@Override
	public void setToDefault(String key) {
		String defaultValue = getDefaultProperty(key);
		setProperty(key, defaultValue);
	}

}
