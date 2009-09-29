package com.mobilesorcery.sdk.ui.editors.resources;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


public abstract class SyntaxColoringPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private ColorManager manager;
	private SyntaxColorPreferenceManager syntaxElements;
	private SyntaxColorPreferenceManager workingCopy;
	private SyntaxColoringPreferenceComposite contents;

	protected Control createContents(Composite parent) {
		manager = new ColorManager(parent.getDisplay());
		
		contents = new SyntaxColoringPreferenceComposite(parent, SWT.NONE);
		syntaxElements = getSyntaxColoringPreferences();
		workingCopy = syntaxElements.getWorkingCopy();
		contents.setSyntaxElements(workingCopy);
		PreviewDocument doc = new PreviewDocument(workingCopy, manager);
		preparePreviewDocument(doc);
		contents.setPreviewDocument(doc);
		return contents;
	}

	public void dispose() {
		manager.dispose();
	}
	
	public void init(IWorkbench workbench) {
		
	}
	
	public boolean performOk() {
		syntaxElements.apply(workingCopy);
		return super.performOk();
	}
	
	public void performDefaults() {
		workingCopy.loadFromDefaults(Activator.getDefault().getPreferenceStore());
		contents.refresh();
		super.performDefaults();
	}

	protected abstract SyntaxColorPreferenceManager getSyntaxColoringPreferences();
	
	protected abstract void preparePreviewDocument(PreviewDocument doc);

}
