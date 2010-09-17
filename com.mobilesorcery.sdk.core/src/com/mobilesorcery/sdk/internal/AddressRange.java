/*  Copyright (C) 2010 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.internal;

import java.util.Comparator;

public class AddressRange {
    
    public final static Comparator<AddressRange> START_COMPARATOR = new Comparator<AddressRange>() {
        public int compare(AddressRange range1, AddressRange range2) {
            return range1.startAddr - range2.startAddr;
        }
    };
    
    public final static Comparator<AddressRange> END_COMPARATOR = new Comparator<AddressRange>() {
        public int compare(AddressRange range1, AddressRange range2) {
            return range1.endAddr - range2.endAddr;
        }
    };
    
    private int startAddr;
    private int endAddr;

    public AddressRange(int startAddr, int endAddr) {
        this.startAddr = startAddr;
        this.endAddr = endAddr;
    }
    
    public boolean inRange(int addr) {
        return addr >= startAddr && addr <= endAddr;
    }
    
    public String toString() {
        return Integer.toHexString(startAddr) + ", " + Integer.toHexString(endAddr);
    }

    public static AddressRange parse(String addressRange) {
        String[] rangeParts = addressRange.split(",", 2);
        if (rangeParts.length == 2) {
            try {
                int startAddr = Integer.parseInt(rangeParts[0].trim(), 16);
                int endAddr = Integer.parseInt(rangeParts[1].trim(), 16);
                return new AddressRange(startAddr, endAddr);
            } catch (NumberFormatException e) {
                // Ignore.
            }
        }
        
        return null;
    }
    
    
}