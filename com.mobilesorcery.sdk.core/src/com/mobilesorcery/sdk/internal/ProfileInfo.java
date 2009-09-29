package com.mobilesorcery.sdk.internal;

import java.util.HashMap;

import org.eclipse.core.runtime.IPath;

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
