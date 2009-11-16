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
/**
 * 
 */
package com.mobilesorcery.sdk.ui.targetphone.internal;

import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.profiles.IProfile;


public class TargetPhone {
    
    public static final int PORT_UNASSIGNED = -1;
    
    private final String name;
    private final byte[] addr;
    private int port;

	private IProfile preferredProfile;

    public TargetPhone(char[] name, byte[] addr, int port) {
        this.name = sz(name);
        this.addr = addr;
        this.port = port;
    }
    
    private String sz(char[] sz) {
        String result = new String(sz);
        int length = result.indexOf('\0');
        return length == -1 ? result : result.substring(0, length);
    }

    public void assignPort(int port) {
        this.port = port;
    }

    public String getName() {
        return name;
    }
    
    public byte[] getAddressAsBytes() {
        return addr;
    }
    
    public String getAddress() {
        return Util.toBase16(addr);
    }
    
    public int getPort() {
        return port;
    }

    public void setPreferredProfile(IProfile preferredProfile) {
		this.preferredProfile = preferredProfile;
	}
    
    public IProfile getPreferredProfile() {
    	return preferredProfile;
    }
 
    public int hashCode() {
        return getAddress().hashCode() ^ getPort();
    }
    
    public boolean equals(Object o) {
        if (o instanceof TargetPhone) {
            return equals((TargetPhone)o);
        }
        
        return false;
    }
    
    public boolean equals(TargetPhone other) {
        if (other == null) {
            return false;
        }
        
        return getAddress().equals(other.getAddress()) && getPort() == other.getPort();
    }
    
    public boolean isPortAssigned() {
        return port != PORT_UNASSIGNED;
    }
    
    public String toString() {
        return getName() + " " + getAddress() + ":" + getPort();
    }


}