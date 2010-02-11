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
 * A default implementation fot property owner working copies.
 * @author Mattias Bybro
 *
 */
public class PropertyOwnerWorkingCopy implements IWorkingCopy {

	private IPropertyOwner original;
	private HashMap<String, String> deferredMap = new HashMap<String, String>(); 

	public PropertyOwnerWorkingCopy(IPropertyOwner original) {
		this.original = original;
	}
	
	public boolean apply() {
		return original.applyProperties(deferredMap);
	}

	public void cancel() {
		deferredMap.clear();
	}

	public boolean applyProperties(Map<String, String> properties) {
		boolean result = false;
		for (String key : properties.keySet()) {
			result |= setProperty(key, properties.get(key));
		}
		return result;
	}

	public String getContext() {
		return original.getContext();
	}

	public String getDefaultProperty(String key) {		
		return original.getDefaultProperty(key);
	}

	public String getProperty(String key) {
		String deferredValue = deferredMap.get(key);
		if (deferredValue == null) {
			return original.getProperty(key);
		}
		
		return deferredValue;
	}

	public void initProperty(String key, String value) {
		throw new UnsupportedOperationException();
	}

	public boolean setProperty(String key, String value) {
		String oldValue = getProperty(key);
		if (!value.equals(oldValue)) {
			deferredMap.put(key, value);
			return true;
		}
		return false;
	}

	public IWorkingCopy createWorkingCopy() {
		return new PropertyOwnerWorkingCopy(this);
	}

	public Map<String, String> getProperties() {
		TreeMap<String, String> result = new TreeMap<String, String>();
		result.putAll(original.getProperties());
		result.putAll(deferredMap);
		return result;
	}

}
