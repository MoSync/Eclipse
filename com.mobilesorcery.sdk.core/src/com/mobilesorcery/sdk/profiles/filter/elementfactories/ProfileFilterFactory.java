/**
 * 
 */
package com.mobilesorcery.sdk.profiles.filter.elementfactories;

import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.filter.IDeviceFilterFactory;
import com.mobilesorcery.sdk.profiles.filter.ProfileFilter;

public class ProfileFilterFactory implements IDeviceFilterFactory {

    public static final String ID = "com.mobilesorcery.mosync.filters.profile";

	public ProfileFilterFactory() {
    }
    
    public IDeviceFilter createFilter(IMemento memento) {
        try {
            ProfileFilter result = new ProfileFilter();
            result.setStyle(memento.getInteger("require"));
            result.setVendors(memento.getString("vendors").split(","));
            result.setProfiles(memento.getString("profiles").split(","));
            return result;
        } catch (Exception e) {
            return null;
        }
        
    }
}