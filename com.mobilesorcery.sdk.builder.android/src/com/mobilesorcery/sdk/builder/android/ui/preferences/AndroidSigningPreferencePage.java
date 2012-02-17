package com.mobilesorcery.sdk.builder.android.ui.preferences;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.Policy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.builder.android.Activator;
import com.mobilesorcery.sdk.builder.android.PropertyInitializer;
import com.mobilesorcery.sdk.builder.java.KeystoreCertificateInfo;
import com.mobilesorcery.sdk.builder.java.ui.KeystoreCertificateInfoEditor;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.PreferenceStorePropertyOwner;
import com.mobilesorcery.sdk.core.SecurePropertyException;
import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;

public class AndroidSigningPreferencePage extends PreferencePage implements IWorkbenchPreferencePage, IUpdatableControl {

    private KeystoreCertificateInfoEditor editor;

    public AndroidSigningPreferencePage() {
        super();
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
    }

    @Override
	public boolean performOk() {
        KeystoreCertificateInfo info = editor.getKeystoreCertInfo();
        try {
			info.store(PropertyInitializer.ANDROID_KEYSTORE_CERT_INFO,
					new PreferenceStorePropertyOwner(getPreferenceStore()),
			    	CoreMoSyncPlugin.getDefault().getSecureProperties());
		} catch (SecurePropertyException e) {
			Policy.getStatusHandler().show(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not encrypt passwords"), "Could not encrypt passwords");
			return false;
		}
		updateUI();
        return true;
    }

    @Override
	public void init(IWorkbench workbench) {
    }

    @Override
	protected Control createContents(Composite parent) {
        editor = new KeystoreCertificateInfoEditor(parent, SWT.NONE);
        editor.setUpdatable(this);
        KeystoreCertificateInfo info = KeystoreCertificateInfo.loadOne(PropertyInitializer.ANDROID_KEYSTORE_CERT_INFO,
        		new PreferenceStorePropertyOwner(getPreferenceStore()),
            	CoreMoSyncPlugin.getDefault().getSecureProperties());
        editor.setKeystoreCertInfo(info);
        setMessage(info.validate(true).getMessage(), info.validate(true).getMessageType());
        updateUI();
        return editor;
    }

	@Override
	public void updateUI() {
		KeystoreCertificateInfo info = editor.getKeystoreCertInfo();
		if (info != null) {
			IMessageProvider message = info.validate(true);
			setMessage(message.getMessage(), message.getMessageType());
		}
	}

}
