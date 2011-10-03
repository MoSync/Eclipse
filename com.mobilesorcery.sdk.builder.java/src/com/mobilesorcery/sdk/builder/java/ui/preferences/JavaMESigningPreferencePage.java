package com.mobilesorcery.sdk.builder.java.ui.preferences;

import java.util.ArrayList;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.builder.java.Activator;
import com.mobilesorcery.sdk.builder.java.KeystoreCertificateInfo;
import com.mobilesorcery.sdk.builder.java.PropertyInitializer;
import com.mobilesorcery.sdk.builder.java.ui.KeystoreCertificateInfoEditor;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.PreferenceStorePropertyOwner;
import com.mobilesorcery.sdk.core.SecurePropertyException;
import com.mobilesorcery.sdk.ui.DefaultMessageProvider;
import com.mobilesorcery.sdk.ui.UpdateListener;
import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;

public class JavaMESigningPreferencePage extends PreferencePage implements IWorkbenchPreferencePage, IUpdatableControl {

    private KeystoreCertificateInfoEditor editor;
    private Button doSign;

    public JavaMESigningPreferencePage() {
        super();
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
    }

    @Override
	public boolean performOk() {
        ArrayList<KeystoreCertificateInfo> infos = new ArrayList<KeystoreCertificateInfo>();
        if (doSign.getSelection()) {
        	infos.add(editor.getKeystoreCertInfo());
        }

        Activator.getDefault().getPreferenceStore().setValue(PropertyInitializer.JAVAME_DO_SIGN, doSign.getSelection());

        try {
			KeystoreCertificateInfo.store(infos,
					PropertyInitializer.JAVAME_KEYSTORE_CERT_INFOS,
					new PreferenceStorePropertyOwner(getPreferenceStore()),
					CoreMoSyncPlugin.getDefault().getSecureProperties());
		} catch (SecurePropertyException e) {
			handleSecurePropertyException(e);
			return false;
		}
        return true;
    }

    private void handleSecurePropertyException(SecurePropertyException e) {
		setMessage(e.getMessage(), IMessageProvider.WARNING);
	}

    private void setMessage(IMessageProvider message) {
    	setMessage(message.getMessage(), message.getMessageType());
    }

    @Override
	public void performDefaults() {

    }

    @Override
	public void init(IWorkbench workbench) {
    }

    @Override
	protected Control createContents(Composite parent) {
        // We only support one certificate at this time (in the UI).
        doSign = new Button(parent, SWT.CHECK);
        doSign.addListener(SWT.Selection, new UpdateListener(this));
        doSign.setText("&Sign application for JavaME platforms");
        editor = new KeystoreCertificateInfoEditor(parent, SWT.NONE);
        KeystoreCertificateInfo info = KeystoreCertificateInfo.loadOne(
        		PropertyInitializer.JAVAME_KEYSTORE_CERT_INFOS,
        		new PreferenceStorePropertyOwner(getPreferenceStore()),
        		CoreMoSyncPlugin.getDefault().getSecureProperties());
        editor.setKeystoreCertInfo(info);
        editor.setUpdatable(this);
        doSign.setSelection(Activator.getDefault().getPreferenceStore().getBoolean(PropertyInitializer.JAVAME_DO_SIGN));

        updateUI();
        return editor;
    }

    @Override
	public void updateUI() {
        editor.setEnabled(doSign.getSelection());
        KeystoreCertificateInfo info = editor.getKeystoreCertInfo();
        setMessage((doSign.getSelection() && info != null) ? info.validate(true) : DefaultMessageProvider.EMPTY);
    }


}
