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

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class SyntaxColorPreferenceManager {

	private HashMap<String, SyntaxColoringPreference> prefs;
	private IPreferenceStore store;

	public SyntaxColorPreferenceManager(SyntaxColoringPreference[] prefs) {
		this(null, prefs);
	}

	public SyntaxColorPreferenceManager(IPreferenceStore store, final SyntaxColoringPreference[] prefs) {
		this.store = store;
		setSyntaxColoringPreferences(prefs);
	}

	public void setSyntaxColoringPreferences(SyntaxColoringPreference[] prefs) {
		this.prefs = new HashMap<String, SyntaxColoringPreference>();
		for (SyntaxColoringPreference pref : prefs) {
			this.prefs.put(pref.getName(), pref);
			if (store != null) {
				pref.loadFrom(store);
			}
		}
	}

	public SyntaxColoringPreference get(String prefId) {
		return prefs.get(prefId);
	}

	public Color getForeground(String prefId) {
		SyntaxColoringPreference pref = get(prefId);
		if (pref == null) {
			return null;
		}

		return getColor(prefId, SyntaxColoringPreference.FG_SUFFIX, pref.getForeground());
	}

	public Color getBackground(String prefId) {
		SyntaxColoringPreference pref = get(prefId);
		if (pref == null) {
			return null;
		}

		return getColor(prefId, SyntaxColoringPreference.BG_SUFFIX, pref.getBackground());
	}

	private Color getColor(String prefId, String suffix, RGB rgb) {
		ColorManager manager = Activator.getDefault().getColorManager(Display.getCurrent());
		return manager.setColor(prefId + suffix, rgb);
	}

	public SyntaxColorPreferenceManager getWorkingCopy() {
		// TODO Should return syntaxcoloringpreferencemanager
		SyntaxColoringPreference[] original = prefs.values().toArray(new SyntaxColoringPreference[0]);
		SyntaxColoringPreference[] copy = new SyntaxColoringPreference[original.length];
		for (int i = 0; i < original.length; i++) {
			copy[i] = original[i].copy();
		}

		SyntaxColorPreferenceManager managerCopy = new SyntaxColorPreferenceManager(copy);
		return managerCopy;
	}

	public void apply(SyntaxColorPreferenceManager workingCopy) {
		SyntaxColoringPreference[] workingCopyPrefs = workingCopy.getAll();
		for (int i = 0; i < workingCopyPrefs.length; i++) {
			applyPref(workingCopyPrefs[i]);
		}
	}

	private void applyPref(SyntaxColoringPreference pref) {
		prefs.put(pref.getName(), pref);
		if (store != null) {
			pref.storeTo(store);
		}
	}

	public SyntaxColoringPreference[] getAll() {
		return prefs.values().toArray(new SyntaxColoringPreference[0]);
	}

	public void loadFromDefaults(IPreferenceStore store) {
		if (store != null) {
			for (SyntaxColoringPreference pref : prefs.values()) {
				pref.loadFromDefaults(store);
			}
		}
	}
}
