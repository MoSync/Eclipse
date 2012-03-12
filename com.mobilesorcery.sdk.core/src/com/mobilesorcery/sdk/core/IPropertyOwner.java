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

public interface IPropertyOwner {

	public interface IWorkingCopy extends IPropertyOwner {
		/**
		 * Applies all changes to the original property owner.
		 * @return <code>true</code> if any changes were actually
		 * made.
		 */
		public boolean apply();

		/**
		 * Cancels any pending changes from this working copy
		 */
		public void cancel();

		/**
		 * Returns the original {@link IPropertyOwner}.
		 * @return
		 */
		public abstract IPropertyOwner getOriginal();
	}

	/**
	 * Sets the value of a property
	 * @param key
	 * @param value null removes property
	 * @return true iff the property was set or removed.
	 * In particular, if
	 * the old value is equal to the new value, <code>false</code>
	 * is returned.
	 */
    public boolean setProperty(String key, String value);

    /**
     * Applies a set of properties
     * @return <code>true</code> if at least one of the properties was
     * different from a previous value
     */
    public boolean applyProperties(Map<String, String> properties);

    public String getProperty(String key);

    public String getDefaultProperty(String key);

    /**
     * Returns <code>true</code> if the default value
     * will be returned for this key.
     * @param key
     * @return
     */
    public boolean isDefault(String key);

    /**
     * See IPropertyInitializer.
     * @return
     */
	public String getContext();

	/**
	 * This method forces a property to be set,
	 * without notifying any listeners or
	 * checking for value changes.
	 * @param key
	 * @param value
	 */
	public void initProperty(String key, String value);

	/**
	 * Returns a working copy of this <code>IPropertyOwner</code>.
	 * Changes can then be applied using the apply() method.
	 * @return
	 */
	public IWorkingCopy createWorkingCopy();

	/**
	 * Returns all properties of this property owner
	 * @return
	 */
	public Map<String, String> getProperties();

	/**
	 * Resets this property owner to reflect the original's
	 * DEFAULT value (however, calling {@link #isDefault(String)}
	 * on the key may not return {@code true}).
	 */
	public void setToDefault(String key);
}
