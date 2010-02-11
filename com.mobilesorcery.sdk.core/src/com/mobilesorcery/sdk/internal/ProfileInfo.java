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
package com.mobilesorcery.sdk.internal;

import java.util.HashMap;

import com.mobilesorcery.sdk.core.IProfileInfo;

public class ProfileInfo implements IProfileInfo {

	private class ProfileInfoKey {
		String file;
		int line;
		
		public ProfileInfoKey(String file, int line) {
			this.file = file;
			this.line = line;
		}
		
		public boolean equals(Object o) {
			if (o instanceof ProfileInfoKey) {
				return equals((ProfileInfoKey) o);
			}
			
			return false;
		}
		
		public boolean equals(ProfileInfoKey other) {
			return file.equals(other.file) && line == other.line;
		}
		
		public int hashCode() {
			return file.hashCode() ^ line;
		}
	}
	
	private HashMap<ProfileInfoKey, Integer> countMap = new HashMap<ProfileInfoKey, Integer>(); 

	public ProfileInfo() {
	}
	
	public int getCount(String file, int line) {
		Integer count = countMap.get(new ProfileInfoKey(file, line));
		return count == null ? 0 : count;
	}

	public void setCount(String file, int line, int count) {
		countMap.put(new ProfileInfoKey(file, line), count);
	}
	
	
}
