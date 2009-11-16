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

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.IVendor;
import com.mobilesorcery.sdk.profiles.filter.elementfactories.VendorFilterFactory;

public class VendorFilter extends AbstractDeviceFilter {

    private HashSet<String> vendors = new HashSet<String>();
    
    public VendorFilter() {
        
    }
    
    public VendorFilter(String[] vendorsToAccept) {
        setVendors(vendorsToAccept);
    }
    
    public void setVendors(String[] vendorsToAccept) {
        vendors.addAll(Arrays.asList(vendorsToAccept));
    }
    
    public void setVendor(String vendorName, boolean include) {
        if (include) {
            vendors.add(vendorName);
        } else {
            vendors.remove(vendorName);
        }
    }
    
    public boolean acceptProfile(IProfile profile) {
        return acceptVendor(profile.getVendor());
    }

    public boolean acceptVendor(IVendor vendor) {
        return vendors.contains(vendor.getName());
    }
    
    public String toString() {
        String vendorList = Util.join(vendors.toArray(new String[0]), ", ");
        String vendorCaption = vendors.size() == 1 ? "Vendor: " : "Vendors: ";
        
        return vendorCaption + vendorList;
    }

    public void saveState(IMemento memento) {
        memento.putString("vendors", Util.join(vendors.toArray(new String[0]), ","));
    }

    public String getFactoryId() {
        return VendorFilterFactory.ID;
    }

}
