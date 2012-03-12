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
package com.mobilesorcery.sdk.core;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.mobilesorcery.sdk.core.IPropertyOwner.IWorkingCopy;

/**
 * A default implementation for property owner working copies.
 * @author Mattias Bybro
 *
 */
public class PropertyOwnerWorkingCopy extends PropertyOwnerBase implements IWorkingCopy {

	private final IPropertyOwner original;
	private final HashMap<String, String> deferredMap = new HashMap<String, String>();

	public PropertyOwnerWorkingCopy(IPropertyOwner original) {
		this.original = original;
	}

	@Override
	public boolean apply() {
		return original.applyProperties(deferredMap);
	}

	@Override
	public void cancel() {
		deferredMap.clear();
	}

	@Override
	public String getContext() {
		return original.getContext();
	}

	@Override
	public String getDefaultProperty(String key) {
		return original.getDefaultProperty(key);
	}

	@Override
	public String getProperty(String key) {
		if (deferredMap.containsKey(key)) {
			return deferredMap.get(key);
		} else {
			return original.getProperty(key);
		}
	}

	@Override
	public void initProperty(String key, String value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setProperty(String key, String value) {
		String oldValue = getProperty(key);
		if (!Util.equals(value, oldValue)) {
			deferredMap.put(key, value);
			return true;
		}
		return false;
	}

	@Override
	public Map<String, String> getProperties() {
		TreeMap<String, String> result = new TreeMap<String, String>();
		result.putAll(original.getProperties());
		result.putAll(deferredMap);
		return result;
	}

	@Override
	public boolean isDefault(String key) {
		return !deferredMap.containsKey(key) && original.isDefault(key);
	}

	@Override
	public IPropertyOwner getOriginal() {
		return original;
	}

}
