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
