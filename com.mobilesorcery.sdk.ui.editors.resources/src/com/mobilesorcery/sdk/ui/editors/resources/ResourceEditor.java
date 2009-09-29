package com.mobilesorcery.sdk.ui.editors.resources;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.AbstractTextEditor;


public class ResourceEditor extends AbstractDecoratedTextEditor {

    private ColorManager manager;
	private IPropertyChangeListener preferenceListener;
    
    public ResourceEditor() {
        super();
        manager = ColorManager.getColorManager(Display.getCurrent());
        final ResourcesFileConfiguration cfg = new ResourcesFileConfiguration(manager);
        setSourceViewerConfiguration(cfg);
        setDocumentProvider(new TextFileDocumentProvider());
        
        preferenceListener = new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				cfg.reinit();
				getSourceViewer().invalidateTextPresentation();
			}
		};
		
		Activator.getDefault().getPreferenceStore().addPropertyChangeListener(preferenceListener);
    }
    
    public void dispose() {
    	Activator.getDefault().getPreferenceStore().removePropertyChangeListener(preferenceListener);
    	super.dispose();
    }
    
}
