package com.mobilesorcery.sdk.builder.iphoneos.ui.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.builder.iphoneos.Activator;
import com.mobilesorcery.sdk.builder.iphoneos.PropertyInitializer;
import com.mobilesorcery.sdk.builder.iphoneos.ui.properties.IPhoneOSCertSelector;

public class IPhoneOSSigningPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private IPhoneOSCertSelector certSelector;

	public IPhoneOSSigningPreferencePage() {
		super();
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}


	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected Control createContents(Composite parent) {
		certSelector = new IPhoneOSCertSelector(parent, SWT.NONE);
		certSelector.setValue(getPreferenceStore().getString(PropertyInitializer.IPHONE_CERT));
		certSelector.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		return certSelector;
	}
	
    public boolean performOk() {
    	getPreferenceStore().setValue(PropertyInitializer.IPHONE_CERT, certSelector.getValue());
    	return true;
    }


}
