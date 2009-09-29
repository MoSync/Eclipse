/**
 * 
 */
package com.mobilesorcery.sdk.profiles.filter.elementfactories;

import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.filter.IDeviceFilterFactory;
import com.mobilesorcery.sdk.profiles.filter.VendorFilter;

public class VendorFilterFactory implements IDeviceFilterFactory {

	public final static String ID = "com.mobilesorcery.mosync.filters.vendor";
	
    public VendorFilterFactory() {
    }
    
    public IDeviceFilter createFilter(IMemento memento) {
        try {
            VendorFilter result = new VendorFilter();
            result.setVendors(memento.getString("vendors").split(","));

            return result;
        } catch (Exception e) {
            return null;
        }
        
    }
    
}