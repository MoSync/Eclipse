package com.mobilesorcery.sdk.profiles;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.Filter;
import com.mobilesorcery.sdk.core.IPackager;
import com.mobilesorcery.sdk.core.MoSyncTool;

public class Profile implements IProfile, Comparable<IProfile> {

    private IVendor vendor;
    private String name;
    private Map<String, Object> properties = new HashMap<String, Object>();
    private String platform;
    private IPackager packager;

    public Profile(IVendor vendor, String name) {
        this.vendor = vendor;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public IVendor getVendor() {
        return vendor;
    }

    public String toString() {
        return MoSyncTool.toString(this);
    }
    
    public int hashCode() {
        return getName().hashCode() ^ getVendor().hashCode();
    }
    
    public boolean equals(Object o) {
        if (o instanceof IProfile) {
            IProfile profile = (IProfile) o;
            return profile.getVendor().equals(this.getVendor()) && profile.getName().equals(this.getName());            
        }
        return false;
    }

    /**
     * Returns a modifiable property list.
     * @return
     */
    public Map<String, Object> getModifiableProperties() {
        return properties;
    }

    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }
    
    public Map<String, Object> getProperties(Filter<String> filter) {
        return Filter.filterMap(getProperties(), filter);
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public IPackager getPackager() {
        return CoreMoSyncPlugin.getDefault().getPackager(platform);
    }

    public int compareTo(IProfile o) {
        return toString().compareTo(o.toString());
    }

    public boolean isEmulator() {
    	return "MobileSorcery/Emulator".equals(toString()); 
    }
}
