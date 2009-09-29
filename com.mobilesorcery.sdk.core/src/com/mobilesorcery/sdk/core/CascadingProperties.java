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
