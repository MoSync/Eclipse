package com.mobilesorcery.sdk.builder.android.ui.properties;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.mobilesorcery.sdk.builder.android.AndroidPackager;
import com.mobilesorcery.sdk.builder.android.PropertyInitializer;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.ui.DefaultMessageProvider;
import com.mobilesorcery.sdk.ui.MoSyncPropertyPage;
import com.mobilesorcery.sdk.ui.UpdateListener;

public class AndroidPropertyPage extends MoSyncPropertyPage {

    public AndroidPropertyPage() {
		super(true);
	}

	private Text packageText;
    private Text versionNumberText;
	private Combo installLocationCombo;
	private FileFieldEditor manifestFile;
	private Button largeHeap;

    @Override
	protected Control createContents(Composite parent) {
        Composite main = new Composite(parent, SWT.NONE);
        main.setLayout(new GridLayout(3, false));

        Label packageLabel = new Label(main, SWT.NONE);
        packageLabel.setText("Android Package name:");
        packageText = new Text(main, SWT.SINGLE | SWT.BORDER);
        packageText.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false, 2, 1));

        Label versionNumberLabel = new Label(main, SWT.NONE);
        versionNumberLabel.setText("Android Version code:");

        versionNumberText = new Text(main, SWT.SINGLE | SWT.BORDER);
        versionNumberText.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false, 2, 1));

        Label installLocationLabel = new Label(main, SWT.NONE);
        installLocationLabel.setText("Android Install Location:");
        
        installLocationCombo = new Combo(main, SWT.READ_ONLY);
        installLocationCombo.setItems(PropertyInitializer.ANDROID_INSTALL_LOCATIONS);
        installLocationCombo.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT, false, false, 2, 1));

        UpdateListener listener = new UpdateListener(this);
        versionNumberText.addListener(SWT.Modify, listener);
        packageText.addListener(SWT.Modify, listener);
        
        manifestFile = new FileFieldEditor("dummy.1", "Custom &manifest template:", main);
        
        largeHeap = new Button(main, SWT.CHECK);
        largeHeap.setText("Use &large heap");
        largeHeap.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT, false, false, 2, 1));
        
        initUI();
        return main;
    }

    private void initUI() {
        setText(packageText, getProject().getProperty(PropertyInitializer.ANDROID_PACKAGE_NAME));
        setText(versionNumberText, Integer.toString(PropertyUtil.getInteger(getProject(), PropertyInitializer.ANDROID_VERSION_CODE)));
        installLocationCombo.setText(getProject().getProperty(PropertyInitializer.ANDROID_INSTALL_LOCATION));
        manifestFile.setStringValue(getProject().getProperty(PropertyInitializer.ANDROID_MANIFEST_TEMPLATE));
        largeHeap.setSelection(PropertyUtil.getBoolean(getProject(), PropertyInitializer.ANDROID_LARGE_HEAP));
    }

    @Override
	public boolean performOk() {
        getProject().setProperty(PropertyInitializer.ANDROID_PACKAGE_NAME, packageText.getText());
        PropertyUtil.setInteger(getProject(), PropertyInitializer.ANDROID_VERSION_CODE, Integer.parseInt(versionNumberText.getText()));
        getProject().setProperty(PropertyInitializer.ANDROID_INSTALL_LOCATION, installLocationCombo.getText());
        getProject().setProperty(PropertyInitializer.ANDROID_MANIFEST_TEMPLATE, manifestFile.getStringValue());
        PropertyUtil.setBoolean(getProject(), PropertyInitializer.ANDROID_LARGE_HEAP, largeHeap.getSelection());
        return true;
    }

    @Override
	public void performDefaults() {
    	setText(packageText, getProject().getDefaultProperty(PropertyInitializer.ANDROID_PACKAGE_NAME));
    	installLocationCombo.setText(PropertyInitializer.ANDROID_INSTALL_LOCATION_DEFAULT);
    	// We do not set the version #
    }

    @Override
	protected void validate() {
        IMessageProvider message = DefaultMessageProvider.EMPTY;
        boolean failedVersionCode = true;
        try {
            int value = Integer.parseInt(versionNumberText.getText());
            failedVersionCode = value < 1;
        } catch (Exception e) {
            // Fall thru.
        }

        if (failedVersionCode) {
            message = new DefaultMessageProvider("Version number must be integer > 0", IMessageProvider.ERROR);
        }

        if (DefaultMessageProvider.isEmpty(message)) {
            message = AndroidPackager.validatePackageName(packageText.getText());
        }

        setMessage(message);
    }
}
