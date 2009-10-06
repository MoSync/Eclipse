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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

public class NewMoSyncProjectPage extends WizardNewProjectCreationPage {

    public NewMoSyncProjectPage() {
        super("MoSyncProjectPage");
        setTitle("MoSync Project");
        setDescription("Create a new MoSync Project");
        setImageDescriptor(ImageDescriptor.createFromFile(this.getClass(), "/icons/new.png"));
    }

}
