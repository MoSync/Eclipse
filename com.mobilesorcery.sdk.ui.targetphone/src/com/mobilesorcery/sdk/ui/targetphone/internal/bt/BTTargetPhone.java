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
package com.mobilesorcery.sdk.ui.targetphone.internal.bt;

import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.ui.targetphone.AbstractTargetPhone;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhone;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhoneTransport;
import com.mobilesorcery.sdk.ui.targetphone.TargetPhonePlugin;


public class BTTargetPhone extends AbstractTargetPhone {
    
    public static final int PORT_UNASSIGNED = -1;
    
    private final byte[] addr;
    private int port;

    public BTTargetPhone(char[] name, byte[] addr, int port) {
        super(sz(name), TargetPhonePlugin.DEFAULT_TARGET_PHONE_TRANSPORT);
        this.addr = addr;
        this.port = port;
    }
    
    private static String sz(char[] sz) {
        String result = new String(sz);
        int length = result.indexOf('\0');
        return length == -1 ? result : result.substring(0, length);
    }

    public void assignPort(int port) {
        this.port = port;
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
 
    public int hashCode() {
        return getAddress().hashCode() ^ getPort();
    }
    
    public boolean equals(Object o) {
        if (o instanceof BTTargetPhone) {
            return equals((BTTargetPhone)o);
        }
        
        return false;
    }
    
    public boolean equals(BTTargetPhone other) {
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