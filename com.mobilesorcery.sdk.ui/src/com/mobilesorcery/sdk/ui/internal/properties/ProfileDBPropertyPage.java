package com.mobilesorcery.sdk.ui.internal.properties;

import org.eclipse.jface.dialogs.MessageDialog;
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
		super(true);
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(UIUtils.newPrefsLayout(1));
		profileTypeButton = new Button(main, SWT.CHECK);
		profileTypeButton
				.setText("Use &platform based profiles (new projects always uses this)");
		Integer pmt = getProject().getProfileManagerType();
		profileTypeButton.setSelection(pmt != null
				&& pmt == MoSyncTool.DEFAULT_PROFILE_TYPE);
		return main;
	}

	@Override
	public boolean performOk() {
		int newProfileManagerType = profileTypeButton
				.getSelection() ? MoSyncTool.DEFAULT_PROFILE_TYPE
				: MoSyncTool.LEGACY_PROFILE_TYPE;
		boolean changeToDeviceBased = getProject().getProfileManagerType() == MoSyncTool.DEFAULT_PROFILE_TYPE &&
				newProfileManagerType == MoSyncTool.LEGACY_PROFILE_TYPE;
		if (!changeToDeviceBased || MessageDialog.openConfirm(getShell(), "Change of profile type", "Changing to using device based profiles may cause some project functionality to stop working. Continue anyway?")) {
			getProject().setProfileManagerType(newProfileManagerType);
			return super.performOk();
		} else {
			return false;
		}
	}

	@Override
	public void performDefaults() {
		profileTypeButton.setSelection(true);
	}

}
