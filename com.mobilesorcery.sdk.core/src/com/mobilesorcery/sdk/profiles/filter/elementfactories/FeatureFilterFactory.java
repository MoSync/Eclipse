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
/**
 * 
 */
package com.mobilesorcery.sdk.profiles.filter.elementfactories;

import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.filter.FeatureFilter;
import com.mobilesorcery.sdk.profiles.filter.IDeviceFilterFactory;

public class FeatureFilterFactory implements IDeviceFilterFactory {

	public static final String ID = "com.mobilesorcery.mosync.filters.feature";

	public FeatureFilterFactory() {
        
    }
    
    public IDeviceFilter createFilter(IMemento memento) {
        try {
            FeatureFilter result = new FeatureFilter();
            result.setStyle(memento.getInteger("require"));
            result.setFeatureIds(memento.getString("feature-ids").split(","));
            return result;
        } catch (Exception e) {
            return null;
        }
        
    }
}