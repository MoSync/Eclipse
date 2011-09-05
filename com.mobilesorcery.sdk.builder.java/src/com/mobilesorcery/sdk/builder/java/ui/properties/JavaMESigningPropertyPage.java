package com.mobilesorcery.sdk.builder.java.ui.properties;

import java.util.ArrayList;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import com.mobilesorcery.sdk.builder.java.Activator;
import com.mobilesorcery.sdk.builder.java.KeystoreCertificateInfo;
import com.mobilesorcery.sdk.builder.java.PropertyInitializer;
import com.mobilesorcery.sdk.builder.java.ui.KeystoreCertificateInfoEditor;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.PreferenceStorePropertyOwner;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.SecurePropertyException;
import com.mobilesorcery.sdk.ui.DefaultMessageProvider;
import com.mobilesorcery.sdk.ui.MoSyncPropertyPage;
import com.mobilesorcery.sdk.ui.UpdateListener;
import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;

public class JavaMESigningPropertyPage extends MoSyncPropertyPage implements IUpdatableControl {

	private Button useProjectSpecific;
	private KeystoreCertificateInfoEditor keyCertUI;
	private Button doSign;
	private Boolean wasProjectSpecificEnabled;
	private KeystoreCertificateInfo projectCertInfo;
	private KeystoreCertificateInfo currentCertInfo;
	private KeystoreCertificateInfo globalCertInfo;
	private UpdateListener listener;

	@Override
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(GridData.FILL_BOTH));

		useProjectSpecific = new Button(main, SWT.CHECK);
		useProjectSpecific.setText("Enable Pr&oject Specific Settings");

		useProjectSpecific.setSelection(PropertyUtil.getBoolean(getProject(),
				PropertyInitializer.JAVAME_PROJECT_SPECIFIC_KEYS));
		listener = new UpdateListener(this);
		useProjectSpecific.addListener(SWT.Selection, listener);

		Label separator = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Group signingGroup = new Group(main, SWT.NONE);
		signingGroup.setText("Signing");
		signingGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		signingGroup.setLayout(new GridLayout(1, false));

		doSign = new Button(signingGroup, SWT.CHECK);
		doSign.setText("&Sign application for JavaME platforms");
		doSign.addListener(SWT.Selection, listener);

		keyCertUI = new KeystoreCertificateInfoEditor(signingGroup, SWT.NONE);
		keyCertUI.setUpdatable(this);
		keyCertUI.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		init();
		doSign.setSelection(projectCertInfo != null);
		updateUI();
		return main;
	}

	private void init() {
		globalCertInfo = KeystoreCertificateInfo.loadOne(
				PropertyInitializer.JAVAME_KEYSTORE_CERT_INFOS,
				new PreferenceStorePropertyOwner(Activator.getDefault()
						.getPreferenceStore()), CoreMoSyncPlugin.getDefault()
						.getSecureProperties());
		projectCertInfo = KeystoreCertificateInfo.loadOne(
				PropertyInitializer.JAVAME_KEYSTORE_CERT_INFOS, getProject(),
				getProject().getSecurePropertyOwner());
		currentCertInfo = globalCertInfo;
	}

	@Override
	public void updateUI() {
		listener.setActive(false);
		boolean isProjectSpecificEnabled = useProjectSpecific.getSelection();

		doSign.setEnabled(isProjectSpecificEnabled);

		boolean changedState = wasProjectSpecificEnabled == null
				|| isProjectSpecificEnabled != wasProjectSpecificEnabled;
		if (changedState) {
			wasProjectSpecificEnabled = isProjectSpecificEnabled;
			if (isProjectSpecificEnabled) {
				keyCertUI.setKeystoreCertInfo(projectCertInfo);
			} else {
				projectCertInfo = currentCertInfo;
				keyCertUI.setKeystoreCertInfo(globalCertInfo);
			}
			// We need to set this explicitly here
			currentCertInfo = keyCertUI.getKeystoreCertInfo();
			doSign.setSelection(currentCertInfo != null);
		}
		keyCertUI.setEnabled(isProjectSpecificEnabled && doSign.getSelection());
		currentCertInfo = keyCertUI.getKeystoreCertInfo();
		setMessage(currentCertInfo == null ? DefaultMessageProvider.EMPTY : currentCertInfo.validate());
		listener.setActive(true);
		super.updateUI();
	}

	private void handleSecurePropertyException(SecurePropertyException e) {
		setMessage(new DefaultMessageProvider(e.getMessage(),
				IMessageProvider.WARNING));
	}

	@Override
	public boolean performOk() {
		PropertyUtil.setBoolean(getProject(),
				PropertyInitializer.JAVAME_PROJECT_SPECIFIC_KEYS,
				useProjectSpecific.getSelection());
		ArrayList<KeystoreCertificateInfo> infos = new ArrayList<KeystoreCertificateInfo>();
		if (doSign.getSelection()) {
			infos.add(keyCertUI.getKeystoreCertInfo());
		}

		if (useProjectSpecific.getSelection()) {
			try {
				KeystoreCertificateInfo.store(infos,
						PropertyInitializer.JAVAME_KEYSTORE_CERT_INFOS,
						getProject(), getProject().getSecurePropertyOwner());
			} catch (SecurePropertyException e) {
				handleSecurePropertyException(e);
				return false;
			}
		}

		return super.performOk();
	}

}
