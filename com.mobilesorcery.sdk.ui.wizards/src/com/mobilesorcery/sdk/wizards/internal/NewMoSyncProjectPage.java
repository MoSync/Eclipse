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
