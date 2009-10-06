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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class CascadingProperties {

	private Map<String, String>[] maps;

	public CascadingProperties(Map<String, String>[] maps) {
		this.maps = maps;
	}
	
	public String get(String key) {
		for (int i = maps.length - 1; i >= 0; i--) {
			String value = maps[i].get(key);
			if (value != null) {
				return value;
			}
		}
		
		return null;
	}

	public Set<String> keySet() {
		HashSet<String> keys = new HashSet<String>();
		for (int i = 0; i < maps.length; i++) {
			keys.addAll(maps[i].keySet());
		}
		
		return keys;
	}
	
	public Map<String, String> toMap() {
	    HashMap<String, String> result = new HashMap<String, String>();
	    for (Iterator<String> keys = keySet().iterator(); keys.hasNext(); ) {
	        String key = keys.next();
	        result.put(key, get(key));
	    }
	    
	    return result;
	}
}
