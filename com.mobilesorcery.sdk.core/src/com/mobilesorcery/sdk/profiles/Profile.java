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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.Filter;
import com.mobilesorcery.sdk.core.IPackager;
import com.mobilesorcery.sdk.core.MoSyncTool;

public class Profile implements IProfile, Comparable<IProfile> {

    private final IVendor vendor;
    private final String name;
    private final Map<String, Object> properties = new HashMap<String, Object>();
    private String platform;
    private IPackager packager;

    public Profile(IVendor vendor, String name) {
        this.vendor = vendor;
        this.name = name;
    }

    @Override
	public String getName() {
        return name;
    }

    @Override
	public IVendor getVendor() {
        return vendor;
    }

    @Override
	public String toString() {
        return MoSyncTool.toString(this);
    }

    @Override
	public int hashCode() {
        return getName().hashCode() ^ getVendor().hashCode();
    }

    @Override
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

    @Override
	public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    @Override
	public Map<String, Object> getProperties(Filter<String> filter) {
        return Filter.filterMap(getProperties(), filter);
    }

    @Override
	public String getRuntime() {
        return platform;
    }

    public void setRuntime(String platform) {
        this.platform = platform;
    }

    @Override
	public IPackager getPackager() {
        return CoreMoSyncPlugin.getDefault().getPackager(platform);
    }

    @Override
	public int compareTo(IProfile o) {
        return toString().compareTo(o.toString());
    }

    @Override
	public boolean isEmulator() {
    	return "MoSync/Emulator".equals(toString());
    }

    public static String getAbbreviatedPlatform(IProfile targetProfile) {
        String platform = targetProfile.getRuntime();
        String abbrPlatform = platform;
        if (platform.length() > "profiles\\runtimes\\".length()) {
        	abbrPlatform = platform.substring("profiles\\runtimes\\".length(), platform.length());
        }
        return abbrPlatform;
    }
}
