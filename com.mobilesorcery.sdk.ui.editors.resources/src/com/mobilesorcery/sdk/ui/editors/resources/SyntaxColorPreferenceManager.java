package com.mobilesorcery.sdk.ui.editors.resources;

import java.util.HashMap;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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
