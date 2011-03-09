package com.mobilesorcery.sdk.builder.java.ui.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

import com.mobilesorcery.sdk.builder.blackberry.BlackBerryPlugin;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.ui.MoSyncPropertyPage;
import com.mobilesorcery.sdk.ui.UIUtils;

public class BlackBerrySigningPropertyPage extends MoSyncPropertyPage implements
		IWorkbenchPropertyPage {

	private Button doSign;

	public BlackBerrySigningPropertyPage() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(UIUtils.newPrefsLayout(1));
		doSign = new Button(main, SWT.CHECK);
		doSign.setText("&Sign BlackBerry Apps");
		init();
		return main;
	}
	
	private void init() {
		doSign.setSelection(PropertyUtil.getBoolean(getProject(), BlackBerryPlugin.PROPERTY_SHOULD_SIGN));
	}
	
	@Override
	public boolean performOk() {
		PropertyUtil.setBoolean(getProject(), BlackBerryPlugin.PROPERTY_SHOULD_SIGN, doSign.getSelection());
		return super.performOk();
	}

}
