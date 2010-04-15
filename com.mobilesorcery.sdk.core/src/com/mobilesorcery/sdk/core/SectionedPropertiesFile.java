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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mobilesorcery.sdk.core.SectionedPropertiesFile.Section.Entry;

/**
 * A utility class for parsing and producing section-based properties files,
 * ie files that look like this:
 * <blockquote><code>
 * [section1]
 * a = b
 * b = c
 * [section2]
 * f = g
 * g = h
 * </code></blockquote>
 * <p>There may exist several sections with the same name.
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 *
 */
public class SectionedPropertiesFile {

	public static class Section {
		public static class Entry {
			
			private String value;
			private String key;

			public static Entry parse(String entry) {
				boolean inEscape = false;				
				StringBuffer currentBuffer = new StringBuffer();
				String key = null;
				String value = null;
				
				char[] chars = entry.toCharArray();
				for (int i = 0; i < chars.length; i++) {
					char ch = chars[i];
					if (ch == '\\' && !inEscape) {
						// Escape
						inEscape = true;
					} else if (ch == '=' && !inEscape) {
						key = currentBuffer.toString().trim();
						currentBuffer = new StringBuffer();
					} else {
						inEscape = false;
						currentBuffer.append(ch);
					}
				}
				
				value = currentBuffer.toString().trim();
				return new Entry(key, value);
			}
			
			public Entry(String key, String value) {
				this.key = key;
				this.value = value;
			}
			
			public String getValue() {
				return value;
			}
			
			public String getKey() {
				return key;
			}
		}
		

		private String name;
		private ArrayList<Entry> entries = new ArrayList<Entry>();

		Section(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		public List<Entry> getEntries() {
			return entries; 
		}
		
		public void addEntry(Entry entry) {
			entries.add(entry);
		}
		
        public void addEntries(Map<String, String> properties) {
            for (String key : properties.keySet()) {
                addEntry(new Entry(key, properties.get(key)));
            }
        }

		public String[] getValues() {
			String[] values = new String[entries.size()];
			for (int i = 0; i < values.length; i++) {
				values[i] = entries.get(i).getValue();
			}
			
			return values;
		}
		
		public Map<String, String> getEntriesAsMap() {
			HashMap<String, String> map = new HashMap<String, String>();
			for (Entry entry : entries) {
				String key = entry.getKey();
				if (key != null) {
					map.put(key, entry.getValue());
				}
			}
			return map;
		}
		
		public String toString() {
			StringBuffer toString = new StringBuffer();
			if (name != null) {
				toString.append("["); //$NON-NLS-1$
				toString.append(name);
				toString.append("]\n"); //$NON-NLS-1$
			}
			
			for (Entry entry : entries) {
				String key = entry.getKey();
				String value = entry.getValue();
				if (key != null) {
					toString.append(unescape(key));
					toString.append(" = "); //$NON-NLS-1$
				}
				
				toString.append(unescape(value));
				toString.append('\n');
			}
			
			return toString.toString();
		}
		
		private String unescape(String str) {
			return str.replace("\\", "\\\\").replace("=", "\\="); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
	}
	
	public static SectionedPropertiesFile parse(File file) throws IOException {
		FileReader reader = new FileReader(file);
		try {
			return parse(reader);
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}
	
	public static SectionedPropertiesFile parse(Reader input) throws IOException {
	    SectionedPropertiesFile result = new SectionedPropertiesFile();
		LineNumberReader lines = new LineNumberReader(input);
		
		Section currentSection = new Section(null);
		
		// TODO: Refactor into separate class.
		for (String line = lines.readLine(); line != null; line = lines.readLine()) {
			String trimmed = line.trim();
			//currentSection
			if (trimmed.startsWith("[")) { //$NON-NLS-1$
				int endIndex = trimmed.indexOf(']');
				if (endIndex != -1) {
					String currentSectionName = trimmed.substring(1, endIndex);
					result.sections.add(currentSection);
					currentSection = new Section(currentSectionName);
				}
			} else if (trimmed.length() > 0) {
				currentSection.addEntry(Entry.parse(line));
			}
		}
	
		result.sections.add(currentSection);
		return result;
	}
	
	public static SectionedPropertiesFile create() {
	    SectionedPropertiesFile result = new SectionedPropertiesFile();
	    result.sections.add(new Section(null));
	    return result;
	}

	private ArrayList<Section> sections = new ArrayList<Section>();

	private SectionedPropertiesFile() {
		
	}
	
	public Section getFirstSection(String name) {
		for (Section section : sections) {
			if (section.getName() == null && name == null) {
				return section;
			}
			if (section.getName() != null && section.getName().equals(name)) {
				return section;
			}
		}
		
		return null;
	}
	
	public Section addSection(String name) {
	    Section section = new Section(name);
	    sections.add(section);
	    return section;
	}
	
	/**
	 * Returns a string representation of this sectioned
	 * properties that can be parsed using the <code>parse</code>
	 * methods.
	 */
	public String toString() {
		StringBuffer toString = new StringBuffer();
		for (Section section : sections) {
			toString.append(section);
			toString.append('\n');
		}
		
		return toString.toString();
	}

	/**
	 * Returns the 'default' section.
	 * Equivalant to call getFirstSection(null)
	 * There is exactly one default section
	 * @return
	 */
	public Section getDefaultSection() {
		return getFirstSection(null);
	}

}
