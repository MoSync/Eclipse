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