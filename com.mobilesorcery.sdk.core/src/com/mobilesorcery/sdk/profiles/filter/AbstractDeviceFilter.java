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
package com.mobilesorcery.sdk.profiles.filter;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;

import org.eclipse.core.runtime.IAdaptable;

import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.IVendor;

public abstract class AbstractDeviceFilter implements IDeviceFilter, IAdaptable {

    public static final int REQUIRE = 0;
    
    public static final int DISALLOW = 1;
    
    private PropertyChangeSupport listeners = new PropertyChangeSupport(this);

    protected boolean required;

    public boolean accept(Object vendorOrProfile) {
        if (vendorOrProfile instanceof IProfile) {
            return acceptProfile((IProfile)vendorOrProfile);
        } else if (vendorOrProfile instanceof IVendor) {
            return acceptVendor((IVendor)vendorOrProfile);
        }
        return false;
    }

    public abstract boolean acceptProfile(IProfile profile);

    /**
     * May be overridden by clients - the default implementation
     * iterates through all the profiles of this vendor
     * and return <code>true</code> if and only if at least
     * one of these profiles is accepted (using the acceptProfile method). 
     * @param vendor
     * @return
     */
    public boolean acceptVendor(IVendor vendor) {
        IProfile[] profiles = vendor.getProfiles();
        for (int i = 0; i < profiles.length; i++) {
            if (acceptProfile(profiles[i])) {
                return true;
            }
        }
        
        return false;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.listeners.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.listeners.addPropertyChangeListener(listener);
    }
    
    /**
     * Subclasses may use this method to notify any listeners about changes.
     * 
     * @param event
     */
    protected void notifyListeners(PropertyChangeEvent event) {
        listeners.firePropertyChange(event);
    }

    public static IVendor[] filterVendors(IVendor[] vendors, IDeviceFilter filter) {
        ArrayList<IVendor> result = new ArrayList<IVendor>();
        for (int i = 0; i < vendors.length; i++) {
            if (filter.accept(vendors[i])) {
                result.add(vendors[i]);
            }
        }
        
        return result.toArray(new IVendor[0]);
    }
    
    public void setStyle(int style) {
        this.required = style == REQUIRE;
    }
    
    public int getStyle() {
        return required ? REQUIRE : DISALLOW;
    }

    public Object getAdapter(Class adapter) {
        if (IDeviceFilter.class.isAssignableFrom(adapter)) {
            return this;
        }
        
        return null;
    }
}
