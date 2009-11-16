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
package com.mobilesorcery.sdk.ui.editors.resources;

import java.text.Collator;
import java.util.Comparator;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;

public class SyntaxColoringPreference {

	public static final Comparator<SyntaxColoringPreference> COMPARATOR = new Comparator<SyntaxColoringPreference>() {
		private Collator collator = Collator.getInstance();

		public int compare(SyntaxColoringPreference pref1, SyntaxColoringPreference pref2) {
			return collator.compare(pref1.getDisplayName(), pref2.getDisplayName());
		}
	};

	public static String FG_SUFFIX = ".fg";
	public static String BG_SUFFIX = ".bg";
	public static String BOLD_SUFFIX = ".b";
	public static String ITALIC_SUFFIX = ".i";
	public static String UNDERLINE_SUFFIX = ".u";
	public static String STRIKETHROUGH_SUFFIX = ".s";

	private String name;
	private String displayName;
	private RGB foreground;
	private RGB background;
	private boolean bold;
	private boolean italic;
	private boolean underline;
	private boolean strikethrough;
	private String category;

	public SyntaxColoringPreference(String category, String name, String displayName) {
		this.category = category;
		this.name = name;
		this.displayName = displayName;
	}

	public SyntaxColoringPreference(String name, String displayName) {
		this(null, name, displayName);
	}

	public SyntaxColoringPreference(SyntaxColoringPreference prototype) {
		this(prototype.getCategory(), prototype.getName(), prototype.getDisplayName());
		this.bold = prototype.isBold();
		this.italic = prototype.isItalic();
		this.underline = prototype.isUnderline();
		this.strikethrough = prototype.isStrikethrough();
		this.background = prototype.getBackground();
		this.foreground = prototype.getForeground();
	}

	public RGB getBackground() {
		return background == null ? PreferenceConverter.COLOR_DEFAULT_DEFAULT : background;
	}

	public void setBackground(RGB background) {
		this.background = background;
	}

	public boolean isBold() {
		return bold;
	}

	public void setBold(boolean bold) {
		this.bold = bold;
	}

	public boolean isItalic() {
		return italic;
	}

	public void setItalic(boolean italic) {
		this.italic = italic;
	}

	public boolean isUnderline() {
		return underline;
	}

	public void setUnderline(boolean underline) {
		this.underline = underline;
	}

	public boolean isStrikethrough() {
		return strikethrough;
	}

	public void setStrikethrough(boolean strikethrough) {
		this.strikethrough = strikethrough;
	}

	public String getName() {
		return name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public RGB getForeground() {
		return foreground == null ? PreferenceConverter.COLOR_DEFAULT_DEFAULT : foreground;
	}

	public void setForeground(RGB foreground) {
		this.foreground = foreground;
	}

	public void storeTo(IPreferenceStore preferences) {
		PreferenceConverter.setValue(preferences, name + FG_SUFFIX, foreground);
		PreferenceConverter.setValue(preferences, name + BG_SUFFIX, background);
		preferences.setValue(name + BOLD_SUFFIX, bold);
		preferences.setValue(name + ITALIC_SUFFIX, italic);
		preferences.setValue(name + UNDERLINE_SUFFIX, underline);
		preferences.setValue(name + STRIKETHROUGH_SUFFIX, strikethrough);
	}
	
	public void storeAsDefaultTo(IPreferenceStore preferences) {
		if (foreground != null) {
			PreferenceConverter.setDefault(preferences, name + FG_SUFFIX, foreground);
		}
		
		if (background != null) {
			PreferenceConverter.setDefault(preferences, name + BG_SUFFIX, background);
		}
		
		preferences.setDefault(name + BOLD_SUFFIX, bold);
		preferences.setDefault(name + ITALIC_SUFFIX, italic);
		preferences.setDefault(name + UNDERLINE_SUFFIX, underline);
		preferences.setDefault(name + STRIKETHROUGH_SUFFIX, strikethrough);		
	}

	public void loadFrom(IPreferenceStore preferences) {
		foreground = PreferenceConverter.getColor(preferences, name + FG_SUFFIX);
		background = PreferenceConverter.getColor(preferences, name + BG_SUFFIX);
		bold = preferences.getBoolean(name + BOLD_SUFFIX);
		italic = preferences.getBoolean(name + ITALIC_SUFFIX);
		underline = preferences.getBoolean(name + UNDERLINE_SUFFIX);
		strikethrough = preferences.getBoolean(name + STRIKETHROUGH_SUFFIX);
	}
	
	public void loadFromDefaults(IPreferenceStore preferences) {
		foreground = PreferenceConverter.getDefaultColor(preferences, name + FG_SUFFIX);
		background = PreferenceConverter.getDefaultColor(preferences, name + BG_SUFFIX);
		bold = preferences.getDefaultBoolean(name + BOLD_SUFFIX);
		italic = preferences.getDefaultBoolean(name + ITALIC_SUFFIX);
		underline = preferences.getDefaultBoolean(name + UNDERLINE_SUFFIX);
		strikethrough = preferences.getDefaultBoolean(name + STRIKETHROUGH_SUFFIX);		
	}

	public String getCategory() {
		return category;
	}

	public String toString() {
		return displayName + " {" + foreground + ", " + background + " - " + (bold ? "BOLD " : "") + (italic ? "ITALIC " : "") + (underline ? "UNDERLINE " : "") + (strikethrough ? "STRIKETHROUGH " : "");
	}
	
	public SyntaxColoringPreference copy() {
		SyntaxColoringPreference result = new SyntaxColoringPreference(this);
		return result;
	}
}