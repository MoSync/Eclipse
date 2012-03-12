package com.mobilesorcery.sdk.builder.iphoneos.ui.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPropertyPage;

import com.mobilesorcery.sdk.builder.iphoneos.PropertyInitializer;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.ui.MoSyncPropertyPage;
import com.mobilesorcery.sdk.ui.UpdateListener;

public class IPhoneOSSigningPropertyPage extends MoSyncPropertyPage implements IWorkbenchPropertyPage {

	public IPhoneOSSigningPropertyPage() {
		super(true);
	}

	private Button useProjectSpecific;
	private IPhoneOSCertSelector certSelector;

	@Override
	protected Control createContents(Composite parent) {
        Composite main = new Composite(parent, SWT.NONE);
        main.setLayout(new GridLayout(1, false));

        useProjectSpecific = new Button(main, SWT.CHECK);
        useProjectSpecific.setText("Enable Pr&oject Specific Settings");

        useProjectSpecific.setSelection(PropertyUtil.getBoolean(getProject(), PropertyInitializer.IPHONE_PROJECT_SPECIFIC_CERT));
        useProjectSpecific.addListener(SWT.Selection, new UpdateListener(this));

        Label separator = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        certSelector = new IPhoneOSCertSelector(main, SWT.NONE);
        certSelector.setValue(getProject().getProperty(PropertyInitializer.IPHONE_CERT));
        certSelector.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        initUI();
        updateUI();

		return main;
	}

    private void initUI() {
        certSelector.setValue(getProject().getProperty(PropertyInitializer.IPHONE_CERT));
        useProjectSpecific.setSelection(PropertyUtil.getBoolean(getProject(), PropertyInitializer.IPHONE_PROJECT_SPECIFIC_CERT));
    }

	@Override
	public void updateUI() {
		certSelector.setEnabled(useProjectSpecific.getSelection());
	}

    @Override
	public boolean performOk() {
        PropertyUtil.setBoolean(getProject(), PropertyInitializer.IPHONE_PROJECT_SPECIFIC_CERT, useProjectSpecific.getSelection());
        getProject().setProperty(PropertyInitializer.IPHONE_CERT, certSelector.getValue());
        return true;
    }

    @Override
	public void performDefaults() {
    	useProjectSpecific.setSelection(PropertyUtil.toBoolean(getProject().getDefaultProperty(PropertyInitializer.IPHONE_PROJECT_SPECIFIC_CERT)));
    	certSelector.setValue(getProject().getDefaultProperty(PropertyInitializer.IPHONE_CERT));
        updateUI();
    }


}
