package com.mobilesorcery.sdk.ui.internal.preferences;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.Policy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.UIUtils;

public class SecurityPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private Combo storageAlternatives;

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(CoreMoSyncPlugin.getDefault().getPreferenceStore());
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(UIUtils.newPrefsLayout(2));
		Label storageLabel = new Label(main, SWT.NONE);
		storageLabel.setText("Store master password:");
		storageAlternatives = new Combo(main, SWT.READ_ONLY);
		storageAlternatives.setItems(new String[] { "In Eclipse Secure Storage", "Do not encrypt" });
		storageAlternatives.select(CoreMoSyncPlugin.getDefault().usesEclipseSecureStorage() ? 0 : 1);
		storageAlternatives.setLayoutData(new GridData(UIUtils.getDefaultFieldSize(), SWT.DEFAULT));
		Text instructions = new Text(main, SWT.WRAP | SWT.READ_ONLY);
		instructions.setBackground(main.getBackground());
		instructions.setText("Sensitive data, such as passwords used for signing, can be encrypted using a master password.\n" +
				"By selecting \"In Eclipse Secure Storage\", the Eclipse secure storage infrastructure will be used for storing the master password.\n" +
				"When \"Do not encrypt\" is selected, sensitive data will not be encrypted at all.\n");
		instructions.setFont(MosyncUIPlugin.getDefault().getFont(MosyncUIPlugin.FONT_INFO_TEXT));
		instructions.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, false, false, 2, 1));
		return main;
	}

	@Override
	public boolean performOk() {
		boolean result = true;
		boolean useEclipseSecureStorage = storageAlternatives.getSelectionIndex() == 0;
		if (useEclipseSecureStorage != CoreMoSyncPlugin.getDefault().usesEclipseSecureStorage()) {
			result = notifyAboutResetMasterPwd();
			if (result) {
				try {
					CoreMoSyncPlugin.getDefault().doUseEclipseSecureStorage(useEclipseSecureStorage);
				} catch (CoreException e) {
					Policy.getStatusHandler().show(e.getStatus(), e.getStatus().getMessage());
				}
			}
		}
		return result;
	}

	private boolean notifyAboutResetMasterPwd() {
		return MessageDialog.openConfirm(getShell(), "Reset master password", "This will reset the master password for this entire workspace including all open projects.");
	}
}
