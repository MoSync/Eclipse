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

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class PropertyUtil {

	private PropertyUtil() {
		
	}

	public static boolean getBoolean(IPropertyOwner p, String key) {
		if (p == null) {
			return false;
		}
		return Boolean.parseBoolean(p.getProperty(key));
	}
	
	public static boolean setBoolean(IPropertyOwner p, String key, boolean value) {
		if (getBoolean(p, key) == value) {
			return false;
		}
		return p.setProperty(key, Boolean.toString(value));
	}
	
	public static String fromBoolean(boolean value) {
	    return Boolean.toString(value);
	}
	
	public static String fromInteger(int value) {
		return Integer.toString(value);
	}
	
    public static IPath[] getPaths(IPropertyOwner p, String key) {
    	if (p == null) {
    		return new IPath[0];
    	}
        return toPaths(p.getProperty(key));
    }
    
    public static void setPaths(IPropertyOwner p, String key, IPath[] paths) {
        p.setProperty(key, fromPaths(paths));
    }
    
    public static IPath[] toPaths(String value) {
		if (value == null) {
			return new IPath[0];
		}
		
    	String[] pathsArray = value.trim().length() == 0 ? new String[0] : value.split(",");
    	ArrayList<IPath> paths = new ArrayList<IPath>();
    	HashSet<String> existingPaths = new HashSet<String>();
    	
    	for (int i = 0; i < pathsArray.length; i++) {
    	    String trimmed = pathsArray[i].trim();
    	    if (!existingPaths.contains(trimmed) && trimmed.length() > 0) {
    	        paths.add(new Path(trimmed));
    	        existingPaths.add(trimmed);
    	    }
    	}

    	return paths.toArray(new IPath[paths.size()]);
	}
	
    public static String fromPaths(IPath[] paths) {
        return Util.join(paths, ", ");
    }
    
    public static String fromPaths(IResource[] resources) {
        IPath[] fullPaths = new IPath[resources.length];
        for (int i = 0; i < resources.length; i++) {
            fullPaths[i] = resources[i].getFullPath();
        }
        
        return fromPaths(fullPaths);
    }

    public static String[] getStrings(IPropertyOwner p, String key) {
    	if (p == null) {
    		return new String[0];
    	}
    	
        String value = p.getProperty(key);
        return toStrings(value);
    }
    
    public static boolean setStrings(IPropertyOwner p, String key, String[] value) {
    	if (p != null) {
    		String valueStr = fromStrings(value);
    		return p.setProperty(key, valueStr);
    	}
    	
    	return false;
    }
    
    public static String[] toStrings(String value) {
        if (value == null) {
            return new String[0];
        }
        
        ArrayList<String> result = new ArrayList<String>();
        char[] chValue = value.toCharArray();
        boolean inEscape = false;
        boolean doAdd = false;
        char[] current = new char[chValue.length];
        int posInCurrent = 0;
        for (int i = 0; i < chValue.length; i++) {
            char ch = chValue[i];
            if (!inEscape && ch =='\\') {
                inEscape = true;
            } else if (!inEscape && ch == ' ') {
                doAdd = true;
            } else if (inEscape || ch != ' '){
                current[posInCurrent] = ch;
                posInCurrent++;
                inEscape = false;
            }
            
            doAdd = doAdd || i == chValue.length - 1;
            
            if (doAdd) {
                String addThis = new String(current, 0, posInCurrent).trim();
                if (addThis.length() > 0) {
                    result.add(new String(current, 0, posInCurrent));
                }
                posInCurrent = 0;
                inEscape = false;
                doAdd = false;
            }
        }
        
        return result.toArray(new String[result.size()]);
    }

    public static String fromStrings(String[] value) {
        for (int i = 0; i < value.length; i++) {
            value[i] = value[i].replace("\\", "\\\\").replace(" ", "\\ ");
        }
        return Util.join(value, " ");
    }
    
    public static Integer getInteger(IPropertyOwner p, String key) {
    	if (p == null) {
    		return null;
    	}
    	
        String value = p.getProperty(key);
        if (value == null) {
            return null;
        }
        
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }
    
    public static int getInteger(IPropertyOwner p, String key, int defaultValue) {
    	Integer value = getInteger(p, key);
    	return value == null ? defaultValue : value;
    }
    
    public static boolean setInteger(IPropertyOwner p, String key, int value) {
        return p.setProperty(key, Integer.toString(value));
    }

    public static String fromObject(Object value) {
        if (value == null) {
            return "";
        } else if (value instanceof IPath[]) {
            return fromPaths((IPath[]) value);
        } else {
            return value.toString();
        }
    }


}

