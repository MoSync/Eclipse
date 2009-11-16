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
