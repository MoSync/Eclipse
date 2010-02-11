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
package com.mobilesorcery.sdk.wizards.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

import com.mobilesorcery.sdk.core.templates.ITemplate;
import com.mobilesorcery.sdk.wizards.Activator;

public class NewMoSyncResourceFilePage extends WizardNewFileCreationPage {

    public NewMoSyncResourceFilePage(String pageName, IStructuredSelection selection) {
        super(pageName, selection);
        setFileExtension("lst");
    }
   
    public InputStream getInitialContents() {
        ITemplate template = Activator.getDefault().getTemplate(Activator.RESOURCE_TEMPLATE_ID);
        String templateContents = ""; // Empty file, worst case.
        
        try {
            templateContents = template.resolve(null);
        } catch (IOException e) {
            // Ignore.
        }
        
        return new ByteArrayInputStream(templateContents.getBytes());
    }


}
