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

public interface IPropertyOwner {

	/**
	 * 
	 * @param key
	 * @param value null removes property
	 * @return true iff the property was set or removed
	 */
    public boolean setProperty(String key, String value);
    public String getProperty(String key);
    public String getDefaultProperty(String key);
    
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
}
