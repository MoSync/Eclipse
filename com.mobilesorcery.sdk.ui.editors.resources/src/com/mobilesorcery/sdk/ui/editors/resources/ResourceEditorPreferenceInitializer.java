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

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class ResourceEditorPreferenceInitializer extends AbstractPreferenceInitializer {

	public ResourceEditorPreferenceInitializer() {
	}

	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		SyntaxColoringPreference directivePref = new SyntaxColoringPreference(ResourcesFileScanner.DIRECTIVE_COLOR, "dummy");
		directivePref.setForeground(ResourcesFileScanner.DIRECTIVE_DEFAULT_RGB);
		directivePref.storeAsDefaultTo(store);

		SyntaxColoringPreference stringPref = new SyntaxColoringPreference(ResourcesFileScanner.STRING_COLOR, "dummy");
		stringPref.setForeground(ResourcesFileScanner.STRING_DEFAULT_RGB);
		stringPref.setBold(true);
		stringPref.storeAsDefaultTo(store);
		
		SyntaxColoringPreference commentPref = new SyntaxColoringPreference(ResourcesFileScanner.COMMENT_COLOR, "dummy");
		commentPref.setForeground(ResourcesFileScanner.COMMENT_DEFAULT_RGB);
		commentPref.setItalic(true);
		commentPref.storeAsDefaultTo(store);

		SyntaxColoringPreference defaultTextPref = new SyntaxColoringPreference(ResourcesFileScanner.DEFAULT_TEXT_COLOR, "dummy");
		defaultTextPref.setForeground(ResourcesFileScanner.DEFAULT_TEXT_DEFAULT_RGB);
		defaultTextPref.storeAsDefaultTo(store);
	}

}
