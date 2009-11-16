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
package com.mobilesorcery.sdk.profiles;

import java.util.ArrayList;
import java.util.TreeMap;

import org.eclipse.jface.resource.ImageDescriptor;


public class Vendor implements IVendor, Comparable<IVendor> {

    private TreeMap<String, IProfile> profiles = new TreeMap<String, IProfile>(String.CASE_INSENSITIVE_ORDER);
    private String name;
    private ImageDescriptor icon;

    public Vendor(String name, ImageDescriptor icon) {
        this.name = name;
        this.icon = icon;
    }

    public ImageDescriptor getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }

    public IProfile[] getProfiles() {
        return profiles.values().toArray(new IProfile[0]);
    }
    
    public void addProfile(IProfile profile) {
        profiles.put(profile.getName(), profile);
    }

    public int hashCode() {
        return getName().hashCode();
    }
    
    public boolean equals(Object o) {
        if (o instanceof IVendor) {
            IVendor vendor = (IVendor) o;
            return (vendor.getName().equals(this.getName()));
        }
        return false;
    }

    public IProfile getProfile(String name) {
        return profiles.get(name);
    }
    
    public String toString() {
        return name;
    }

    public int compareTo(IVendor o) {
        return getName().compareTo(o.getName());
    }
}
