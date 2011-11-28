package com.mobilesorcery.sdk.ui.internal.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.ui.MoSyncPropertyPage;
import com.mobilesorcery.sdk.ui.UIUtils;

public class ProfileDBPropertyPage extends MoSyncPropertyPage {

	private Button profileTypeButton;

	public ProfileDBPropertyPage() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(UIUtils.newPrefsLayout(1));
		profileTypeButton = new Button(main, SWT.CHECK);
		profileTypeButton
				.setText("Use &platform based profiles (new projects always uses this)");
		Integer pmt = PropertyUtil.getInteger(getProject(),
				MoSyncProject.PROFILE_MANAGER_TYPE_KEY);
		profileTypeButton.setSelection(pmt != null
				&& pmt == MoSyncTool.DEFAULT_PROFILE_TYPE);
		return main;
	}

	@Override
	public boolean performOk() {
		PropertyUtil.setInteger(getProject(),
				MoSyncProject.PROFILE_MANAGER_TYPE_KEY, profileTypeButton
						.getSelection() ? MoSyncTool.DEFAULT_PROFILE_TYPE
						: MoSyncTool.LEGACY_PROFILE_TYPE);
		return super.performOk();
	}

}
