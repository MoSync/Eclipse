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

import org.eclipse.jface.preference.IPreferenceStore;


public class ResourceEditorPreferencePage extends SyntaxColoringPreferencePage {

	public ResourceEditorPreferencePage() {
	}

	public IPreferenceStore getPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();		
	}

	protected SyntaxColorPreferenceManager getSyntaxColoringPreferences() {
		return Activator.getDefault().getSyntaxColorPreferenceManager();
	}

	protected void preparePreviewDocument(PreviewDocument doc) {
		doc.addSnippet(ResourcesFileScanner.COMMENT_COLOR, "/*\nResource File\n  Created Jul 27, 2010\n*/");
		doc.addSnippet(ResourcesFileScanner.DIRECTIVE_COLOR, "\n.res ");
		doc.addSnippet(ResourcesFileScanner.DEFAULT_TEXT_COLOR, "R_IMAGE");
		doc.addSnippet(ResourcesFileScanner.DIRECTIVE_COLOR, "\n.image ");
		doc.addSnippet(ResourcesFileScanner.STRING_COLOR, "\"image.png\"\n");
		doc.addSnippet(ResourcesFileScanner.COMMENT_COLOR, "// End Of File\n");		
		doc.addSnippet(ResourcesFileScanner.DIRECTIVE_COLOR, "\n.eof");		
	}

}
