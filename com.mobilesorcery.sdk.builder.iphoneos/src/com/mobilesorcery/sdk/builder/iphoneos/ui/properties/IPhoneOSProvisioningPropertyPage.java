package com.mobilesorcery.sdk.builder.iphoneos.ui.properties;

import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPropertyPage;

import com.mobilesorcery.sdk.builder.iphoneos.PropertyInitializer;
import com.mobilesorcery.sdk.ui.MoSyncPropertyPage;

public class IPhoneOSProvisioningPropertyPage extends MoSyncPropertyPage implements IWorkbenchPropertyPage {

	private FileFieldEditor provisioningFile;

	public IPhoneOSProvisioningPropertyPage() {
		super(true);
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		provisioningFile = new FileFieldEditor("", "Provisioning file:", main);
		addContentAssist(provisioningFile.getTextControl(main));
		provisioningFile.setFileExtensions(new String[] { "*.mobileprovision", "*.*" });
		provisioningFile.setStringValue(getProject().getProperty(PropertyInitializer.IOS_PROVISIONING_FILE));
		return main;
	}
	
	@Override
	public boolean performOk() {
    	getProject().setProperty(PropertyInitializer.IOS_PROVISIONING_FILE, provisioningFile.getStringValue());
        return true;
    }

    @Override
	public void performDefaults() {
    	provisioningFile.setStringValue(getProject().getDefaultProperty(PropertyInitializer.IOS_PROVISIONING_FILE));
    }

}
