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
package com.mobilesorcery.sdk.core;

/**
 * <p>A class representing the most common ways to write versions. This class can handle
 * these kinds of version representations:</p>
 * <ul>
 * <li>1</li>
 * <li>1.0</li>
 * <li>1.0.0</li>
 * <li>1.0b</li>
 * <li>1.0.0b</li>
 * <li>1.0.0.b</li>
 * </ul>
 * <p>In all the above examples, "b" is retrieved using the method <code>getQualifier()</code><p>
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 *
 */
public class Version {

    public static final int UNDEFINED = -1;
    
    public static final int MAJOR = 1;
    public static final int MINOR = 2;
    public static final int MICRO = 3;
    public static final int QUALIFIER = 4;
    
    private int major = UNDEFINED;
    private int minor = UNDEFINED;
    private int micro = UNDEFINED;
    private String qualifier;

    private String version;

    private boolean valid = true;

    public Version(String version) {
        this.version = version;
        parse(version);
    }

    private void parse(String version) {
        String[] components = version.split("\\.", 4);
        major = parseInternal(components, 0);
        minor = parseInternal(components, 1);
        micro = parseInternal(components, 2);
        parseInternal(components, 3);
        valid &= major != UNDEFINED;
    }

    private int parseInternal(String[] components, int ix) {
        if (components.length <= ix) {
            return UNDEFINED;
        }
        
        int intIx = 0;
        while (intIx < components[ix].length() && Character.isDigit(components[ix].charAt(intIx))) {
            intIx++;
        }
            
        // Qualifier is whatever follows a number, or if non-numeric.
        if (Util.isEmpty(qualifier) && components[ix].length() > intIx - 1 || intIx == 0) {
            char[] qualifier = components[ix].substring(intIx).toCharArray();
            for (int i = 0; i < qualifier.length; i++) {
                // We replace any non-alphanumeric character with an underscore
                if (!Character.isLetterOrDigit(qualifier[i])) {
                    qualifier[i] = '_';
                    valid = false;
                }
            }
            
            this.qualifier = new String(qualifier);
        }
        
        return intIx == 0 ? UNDEFINED : Integer.parseInt(components[ix].substring(0, intIx));
    }
    
    public String toString() {
        return version;
    }
    
    public String asCanonicalString() {
        return asCanonicalString(QUALIFIER);
    }
    
    public String asCanonicalString(int level) {
        StringBuffer result = new StringBuffer();
        if (level >= MAJOR) addIfDefined(result, false, major);
        if (level >= MINOR) addIfDefined(result, true, minor);
        if (level >= MICRO) addIfDefined(result, true, micro);
        if (level >= QUALIFIER && !Util.isEmpty(qualifier)) {
            result.append('.' + qualifier);
        }
        
        return result.toString();
    }

    private void addIfDefined(StringBuffer result, boolean addDelimiter, int major) {
        if (major != UNDEFINED) {
            if (addDelimiter) {
                result.append('.');
            }
            result.append(major);
        }
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }
    
    public int getMicro() {
        return micro;
    }
    
    public String getQualifier() {
        return qualifier;
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public boolean equals(Object o) {
        if (o instanceof Version) {
            return equals((Version) o);
        }
        
        return false;
    }
    
    public boolean equals(Version v) {
        return getMajor() == v.getMajor() &&
               getMinor() == v.getMinor() &&
               getMicro() == v.getMicro() &&
               Util.equals(getQualifier(), v.getQualifier());
    }
    
    /**
     * Returns whether this version is 
     * "before" another version; ie when 
     * it can be concluded from the major, minor, micro
     * whether this version is older than
     * another. (So the qualifier is always disregarded).
     * @return
     */
    public boolean isOlder(Version other) {
        if (other == null) {
            return false;
        }
        if (getMajor() < other.getMajor()) {
            return true;
        } else if (getMajor() == other.getMajor()) {
            if (getMinor() < other.getMinor()) {
                return true;
            } else if (getMinor() == other.getMinor()) {
                return getMicro() < other.getMicro();
            }
        }
        
        return false;
    }
    
    /**
     * Returns whether this version is 
     * "after" another version; ie when 
     * it can be concluded from the major, minor, micro
     * whether this version is newer than
     * another. (So the qualifier is always disregarded).
     * @return
     */
    public boolean isNewer(Version other) {
        if (other == null) {
            return false;
        }
        if (getMajor() > other.getMajor()) {
            return true;
        } else if (getMajor() == other.getMajor()) {
            if (getMinor() > other.getMinor()) {
                return true;
            } else if (getMinor() == other.getMinor()) {
                return getMicro() > other.getMicro();
            }
        }
        
        return false;
    }

}
