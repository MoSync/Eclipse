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