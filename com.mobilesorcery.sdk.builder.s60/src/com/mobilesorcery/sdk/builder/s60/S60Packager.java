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
package com.mobilesorcery.sdk.builder.s60;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mobilesorcery.sdk.core.AbstractPackager;
import com.mobilesorcery.sdk.core.Version;
import com.mobilesorcery.sdk.profiles.IProfile;

public abstract class S60Packager extends AbstractPackager {

    private static final String SYMBIAN_OS_VERSION_PREFIX = "MA_PROF_SUPPORT_OS_SYMBIAN_";
    
    private static final Pattern SYMBIAN_OS_PATTERN = Pattern.compile("\\D*(\\d*)\\D*(\\d*)");

    private static final Version SYMBIAN_S60V5_OS_VERSION = new Version("9.4");

    static byte[] readFile(File file) throws IOException {
        DataInputStream input = null;
        byte[] buffer = new byte[(int) file.length()];
        
        try {
            input = new DataInputStream(new FileInputStream(file));
            input.readFully(buffer);
        } finally {
            if (input != null) {
                input.close();
            }
        }
        
        return buffer;
    }
    
    static void writeFile(File file, byte[] buffer) throws IOException {
        FileOutputStream output = new FileOutputStream(file);        
        try {
            output.write(buffer);
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }
    
    static int readInt(byte[] buffer, int offset) {
        return (buffer[offset] & 0xff) |
        ((buffer[offset + 1] & 0xff) << 8) |
        ((buffer[offset + 2] & 0xff) << 16) |
        ((buffer[offset + 3] & 0xff) << 24);
    }
    
    /**
     * Writes a 32-bit integer to a Symbian EXE image (little-endian)
     * @param value
     * @param buffer
     * @param offset
     */
    static void writeInt(int value, byte[] buffer, int offset) {
        buffer[offset + 3] = (byte)((value >> 24) & 0xff);
        buffer[offset + 2] = (byte)((value >> 16) & 0xff);
        buffer[offset + 1] = (byte)((value >> 8) & 0xff);
        buffer[offset] = (byte)(value & 0xff);
    }
    
    /**
     * Tries to deduce the Symbian OS version of a profile,
     * or <code>null</code> if none was found.
     * @param profile
     * @return
     */
    public static Version getOSVersion(IProfile profile) {
        Map<String, Object> props = profile.getProperties();
        if ("true".equalsIgnoreCase("" + props.get("MA_PROF_STRING_PLATFORM_S60V5"))) {
            return SYMBIAN_S60V5_OS_VERSION;
        }
        
        for (Entry<String, Object> entry : props.entrySet()) {
            if (entry.getKey().startsWith(SYMBIAN_OS_VERSION_PREFIX)) {
                return getOSVersion(entry.getKey());
            }
        }
        
        return null;
    }
    
    private static Version getOSVersion(String osVersionProperty) {
        String suffix = osVersionProperty.substring(SYMBIAN_OS_VERSION_PREFIX.length());
        Matcher matcher = SYMBIAN_OS_PATTERN.matcher(suffix);
        if (matcher.matches()) {
            String major = matcher.group(1);
            String minor = matcher.group(2);
            return new Version(major + "." + minor);
        }
        
        return null;
    }
   
}
