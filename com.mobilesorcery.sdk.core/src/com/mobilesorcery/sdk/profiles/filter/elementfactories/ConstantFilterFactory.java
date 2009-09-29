/**
 * 
 */
package com.mobilesorcery.sdk.profiles.filter.elementfactories;

import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.filter.ConstantFilter;
import com.mobilesorcery.sdk.profiles.filter.IDeviceFilterFactory;

public class ConstantFilterFactory implements IDeviceFilterFactory {

	public final static String ID = "com.mobilesorcery.mosync.filters.constant";
	
	public ConstantFilterFactory() {

    }

    public IDeviceFilter createFilter(IMemento memento) {
        try {
            ConstantFilter result = new ConstantFilter();
            result.setConstantFeature(memento.getString("constant-feature"));
            result.setRelationalOp(ConstantFilter.getOp(memento.getString("op")));
            result.setThreshold(Long.parseLong(memento.getString("threshold")));

            return result;
        } catch (Exception e) {
            return null;
        }
        
    }
}