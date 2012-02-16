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

import java.util.Map;
import java.util.TreeMap;

/**
 * <p>A 'namespace' property owner, where all property keys are suffixed
 * with a namespace using the / character.<p>
 * <p>Namespaces have a natural
 * hierarchy - the value for key <code>property/overridden</code>
 * will first be found using the exact key, <code>property/overridden</code>.
 * If no value is found, <code>property</code> will be used as a key. If
 * still no value is found, the same procedure is repeated with default values.
 * </p>
 * <p>
 * Likewise, the key <code>property/overridden/overriddenagain</code> will
 * potentially use an additional lookup.
 * <emph>TODO: Use something like CascadingProperties instead, much less bookkeeping!!</emph>
 * @author Mattias Bybro
 *
 */
public class NameSpacePropertyOwner extends PropertyOwnerBase implements IPropertyOwner {

	private final IPropertyOwner parent;
	private final String[] namespace;
	private final int levels;
	private final String fullNamespace;

	public NameSpacePropertyOwner(IPropertyOwner parent, String namespace) {
		this.parent = parent;
		this.namespace = namespace.split("/");
		this.fullNamespace = namespace;
		this.levels = this.namespace.length;
	}

	public String getNamespace() {
		return fullNamespace;
	}

	private String assembleFullKey(String key, int level) {
		return assembleFullKeys(key)[level];
	}

	private String[] assembleFullKeys(String key) {
		String[] result = new String[levels + 1];
		StringBuffer buffer = new StringBuffer(key);
		result[0] = buffer.toString();
		for (int i = 0; i < levels; i++) {
			buffer.append('/');
			buffer.append(namespace[i]);
			result[i + 1] = buffer.toString();
		}
		return result;
	}

	@Override
	public String getContext() {
		return parent.getContext();
	}

	@Override
	public String getDefaultProperty(String key) {
		String[] fullKeys = assembleFullKeys(key);
		for (int i = 0; i < levels + 1; i++) {
			String prop = parent.getDefaultProperty(fullKeys[levels - i]);
			if (prop != null) {
				return prop;
			}
		}

		return null;
	}

	@Override
	public String getProperty(String key) {
		String[] fullKeys = assembleFullKeys(key);
		for (int i = 0; i < levels + 1; i++) {
			String prop = parent.getProperty(fullKeys[levels - i]);
			if (prop != null) {
				return prop;
			}
		}

		return null;
	}

	@Override
	public void initProperty(String key, String value) {
		parent.initProperty(assembleFullKey(key, levels), value);
	}

	@Override
	public boolean setProperty(String key, String value) {
		if (equals(getProperty(key), value)) {
			return false;
		} else {
			return parent.setProperty(assembleFullKey(key, levels), value);
		}
	}

	public static boolean equals(String oldValue, String value) {
		if (oldValue == null) {
			return value == null;
		}

		return oldValue.equals(value);
	}

	/**
	 * Returns the key part of a namespaced key
	 * @param key
	 * @param fullKey
	 * @return
	 */
	public static String getKey(String key) {
		String[] parts = key.split("/", 2);
		if (parts.length == 0) {
			return "";
		} else {
			return parts[0];
		}
	}

	public static String[] getNamespace(String key) {
		String[] parts = key.split("/");
		if (parts.length == 1) {
			return new String[] { "" };
		} else {
			String[] result = new String[parts.length - 1];
			System.arraycopy(parts, 1, result, 0, result.length);
			return result;
		}
	}

	public static String getFullNamespace(String key) {
		String[] parts = key.split("/", 2);
		if (parts.length == 1) {
			return "";
		} else {
			return parts[1];
		}
	}

	public Map<String, String> getProperties(String fullNamespace) {
		TreeMap<String, String> result = new TreeMap<String, String>();
		Map<String, String> parentProperties = parent.getProperties();
		for (String key : parentProperties.keySet()) {
			if (fullNamespace.equals(getFullNamespace(key))) {
				result.put(getKey(key), parentProperties.get(key));
			}
		}

		return result;
	}

	@Override
	public Map<String, String> getProperties() {
	    Map<String, String> result = parent.getProperties();
	    result.putAll(getProperties(fullNamespace));
	    return result;
	}

	/**
	 * Clears all properties with this namespace from the parent.
	 */
	public void clear() {
		// A tad slow, but this method is rarely used.
		Map<String, String> properties = getProperties(fullNamespace);
		for (String key : properties.keySet()) {
			setProperty(key, null);
		}
	}

	@Override
	public boolean isDefault(String key) {
		String[] fullKeys = assembleFullKeys(key);
		for (int i = 0; i < levels + 1; i++) {
			boolean isDefault = parent.isDefault(fullKeys[levels - i]);
			if (!isDefault) {
				return false;
			}
		}
		return true;
	}

}
