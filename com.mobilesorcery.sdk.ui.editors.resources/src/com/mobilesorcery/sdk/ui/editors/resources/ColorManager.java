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
package com.mobilesorcery.sdk.ui.editors.resources;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

// TODO: MOVE TO COLORMANAGER WITH KEYS - SHOULD HAVE ONE SOMEWHERE
public class ColorManager {

	private static Map<Display, ColorManager> managers = new HashMap<Display, ColorManager>();

	protected Map fColorTable = new HashMap(10);

	private Display display;

	ColorManager(final Display display) {
		this.display = display;
	}
		
	public static ColorManager getColorManager(Display display) {
		// TODO: move to plugin activator.
		ColorManager manager = (ColorManager) managers.get(display);
		if (manager == null) {
			manager = new ColorManager(display);
		}
		
		return manager;
	}
	
	public static void disposeAll() {
		for (ColorManager manager : managers.values()) {
			manager.dispose();
		}
		managers.clear();
	}
	
	void dispose() {
		Iterator e = fColorTable.values().iterator();
		while (e.hasNext())
			 ((Color) e.next()).dispose();
	}
	
	public void dispose(String key) {
		if (key == null) {
			return;
		}
		
		Color color = (Color) fColorTable.remove(key);
		if (color != null) {
			color.dispose();
		}
	}
	
	public Color getColor(String key) {
		Color color = (Color) fColorTable.get(key);
		return color;
	}
	
	public Color setColor(String key, RGB rgb) {
		if (rgb == null) {
			dispose(key);
			return null;
		}
		
		Color oldColor = getColor(key);
		
		if (oldColor != null && !oldColor.isDisposed() && rgb.equals(oldColor.getRGB())) {
			return oldColor;
		}
		
		dispose(key);
		Color color = new Color(display, rgb);
		fColorTable.put(key, color);
		return color;
	}

	public void loadColorsFromPreferences(IPreferenceStore store, String[] keys) {
		for (int i = 0; i < keys.length; i++) {
			RGB rgb = PreferenceConverter.getColor(store, keys[i]);
			setColor(keys[i], rgb);
		}
	}
}
