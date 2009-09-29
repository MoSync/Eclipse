package com.mobilesorcery.sdk.ui.editors.resources;

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


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
