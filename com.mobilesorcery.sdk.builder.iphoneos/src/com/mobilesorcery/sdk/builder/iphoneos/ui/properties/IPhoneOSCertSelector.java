package com.mobilesorcery.sdk.builder.iphoneos.ui.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class IPhoneOSCertSelector extends Composite {

	private Combo certFieldWithHints;
	private Label label;

	public IPhoneOSCertSelector(Composite parent, int style) {
		super(parent, style);
		init();
	}

	private void init() {
		setLayout(new GridLayout(2, false));
		label = new Label(this, SWT.NONE);
		label.setText("Certificate name:");
		
		certFieldWithHints = new Combo(this, SWT.NONE);
		// The two most common cert names:
		certFieldWithHints.add("iPhone Developer");
		certFieldWithHints.add("iPhone Distribution");
		certFieldWithHints.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	public void setValue(String value) {
		certFieldWithHints.setText(value == null ? "" : value);	
	}
	
	public String getValue() {
		return certFieldWithHints.getText();
	}
	
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		certFieldWithHints.setEnabled(enabled);
		label.setEnabled(enabled);
	}

}
