package com.mobilesorcery.sdk.wizards.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
