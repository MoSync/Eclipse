/**
 * 
 */
package com.mobilesorcery.sdk.fontsupport.internal.wizard;

import java.util.HashMap;

// NOT NECESSARY -- REMOVE
public class BMFontInfoBlock {
	public static final String PAGE_NAME = "page.name"; //$NON-NLS-1$
	
	private HashMap<String, Object> map = new HashMap<String, Object>();

	public int type;
	
	public BMFontInfoBlock(int type) {
		this.type = type;
	}

	public void setString(String key, String value) {
		map.put(key, value);
	}
	
	public int getType() {
		return type;
	}
	
	public boolean isType(int type) {
		return this.type == type;
	}
	
	public String toString() {
		return type + "; " + map; //$NON-NLS-1$
	}

	public String getString(String key) {
		return (String) map.get(key);
	}
}