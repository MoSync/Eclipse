package com.mobilesorcery.sdk.builder.iphoneos.ui.properties;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

import com.mobilesorcery.sdk.builder.iphoneos.PropertyInitializer;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.ui.DefaultMessageProvider;
import com.mobilesorcery.sdk.ui.MoSyncPropertyPage;
import com.mobilesorcery.sdk.ui.UpdateListener;

public class IPhoneOSPropertyPage extends MoSyncPropertyPage implements IWorkbenchPropertyPage {

	private Text bundleIdentifier;

	public IPhoneOSPropertyPage() {
		super(true);
	}
	
	@Override
	protected Control createContents(Composite parent) {
        Composite main = new Composite(parent, SWT.NONE);
        main.setLayout(new GridLayout(2, false));

        Label bundleIdentifierLabel = new Label(main, SWT.NONE);
        bundleIdentifierLabel.setText("Bundle identifier");
        
        bundleIdentifier = new Text(main, SWT.SINGLE | SWT.BORDER);
        bundleIdentifier.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
        bundleIdentifier.addListener(SWT.Modify, new UpdateListener(this));
        addContentAssist(bundleIdentifier);

        initUI();
        updateUI();

		return main;
	}

    private void initUI() {
    	setText(bundleIdentifier, getProject().getProperty(PropertyInitializer.IOS_BUNDLE_IDENTIFIER));
    }

    public void validate() {
    	String bundleId = bundleIdentifier.getText();
    	String message = null;
    	if (Util.isEmpty(bundleId)) {
    		message = "Bundle identifier cannot be empty";
    	} else if (bundleId.contains("..")) {
    		message = "Bundle identifier cannot have empty segment";
    	} else if (bundleId.endsWith(".")) {
    		message = "Bundle identifier cannot end with '.'";
    	} else if (containsInvalidChars(bundleId)) {
    		message = "Bundle identifier segments can only contain letters, digits and '-'.";
    	}
		setMessage(new DefaultMessageProvider(message, message == null ? IMessageProvider.NONE : IMessageProvider.ERROR));
    }

    private boolean containsInvalidChars(String bundleId) {
		for (int i = 0; i < bundleId.length(); i++) {
			char ch = bundleId.charAt(i);
			boolean valid = ch > 32 && ch < 128 && (Character.isLetter(ch) || Character.isDigit(ch) || ch == '-' || ch == '.' || ch == '%');
			if (!valid) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean performOk() {
    	getProject().setProperty(PropertyInitializer.IOS_BUNDLE_IDENTIFIER, bundleIdentifier.getText());
        return true;
    }

    @Override
	public void performDefaults() {
    	setText(bundleIdentifier, getProject().getDefaultProperty(PropertyInitializer.IOS_BUNDLE_IDENTIFIER));
    }

}
