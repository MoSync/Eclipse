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
package com.mobilesorcery.sdk.ui.internal.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;

/**
 * Root level of the preferences, the "MoSync Tool" options.
 */
public class MainPreferencePage extends PreferencePage implements IWorkbenchPreferencePage
{
	/**
	 * Label that shows a descriptive text.
	 */
    private Label m_descText;

    public MainPreferencePage()
    {
        super("MoSync", CoreMoSyncPlugin.getImageDescriptor("/icons/mosyncproject.png"));
        
        // This is the root options, and there is no settings here.
        noDefaultAndApplyButton();
        
        IPreferenceStore store = CoreMoSyncPlugin.getDefault().getPreferenceStore();
        setPreferenceStore(store);
    }
    
    /**
     * @see PreferencePage.
     */
    protected Control createContents(Composite parent)
    {
    	m_descText = new Label(parent, SWT.TOP | SWT.LEFT);
    	m_descText.setText("Expand the tree to the left to edit preferences for a specific feature.");
    	
    	return parent;
    }

    /**
     * @see IWorkbenchPreferencePage.
     */
    public void init(IWorkbench workbench)
    {
    	
    }
}
