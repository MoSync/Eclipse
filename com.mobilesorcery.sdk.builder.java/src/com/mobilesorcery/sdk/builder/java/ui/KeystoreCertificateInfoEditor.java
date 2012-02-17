package com.mobilesorcery.sdk.builder.java.ui;

import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.mobilesorcery.sdk.builder.java.KeystoreCertificateInfo;
import com.mobilesorcery.sdk.ui.PasswordTextFieldDecorator;
import com.mobilesorcery.sdk.ui.UpdateListener;
import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;

public class KeystoreCertificateInfoEditor extends Composite implements IUpdatableControl {

    private final FileFieldEditor keyStore;
    private final Text passkey;
    private final Label passkeyLabel;
    private final Text storekey;
    private final Label storekeyLabel;
    private final Label aliasLabel;
    private final Text alias;
    private final PasswordTextFieldDecorator storekeyDec;
    private final PasswordTextFieldDecorator passkeyDec;
    private KeystoreCertificateInfo info;
	private boolean dirty = false;
	private IUpdatableControl updatable;
	private final UpdateListener listener;

    public KeystoreCertificateInfoEditor(Composite parent, int style) {
        super(parent, style);

        keyStore = new FileFieldEditor("dummy.1", "Key&store", this) {  //$NON-NLS-1$
        	@Override
			protected boolean checkState() {
        		clearErrorMessage();
        		return true;
        	}
        };

        storekeyLabel = new Label(this, SWT.PASSWORD);
        storekeyLabel.setText("&Keystore password");

        storekey = new Text(this, SWT.BORDER | SWT.SINGLE);
        storekeyDec = new PasswordTextFieldDecorator(storekey);
        GridData storekeyData = new GridData(GridData.FILL_HORIZONTAL);
        storekeyData.horizontalSpan = 2;
        storekey.setLayoutData(storekeyData);

        aliasLabel = new Label(this, SWT.NONE);
        aliasLabel.setText("Key &Alias");

        alias = new Text(this, SWT.BORDER | SWT.SINGLE);
        GridData aliasData = new GridData(GridData.FILL_HORIZONTAL);
        aliasData.horizontalSpan = 2;
        alias.setLayoutData(aliasData);

        passkeyLabel = new Label(this, SWT.NONE);
        passkeyLabel.setText("&Private key password");

        passkey = new Text(this, SWT.BORDER | SWT.SINGLE);
        passkeyDec = new PasswordTextFieldDecorator(passkey);
        GridData passkeyData = new GridData(GridData.FILL_HORIZONTAL);
        passkeyData.horizontalSpan = 2;
        passkey.setLayoutData(passkeyData);

        listener = new UpdateListener(this);
        listener.addTo(SWT.Modify, keyStore.getTextControl(this), storekey, alias, passkey);
    }

    @Override
	public void setEnabled(boolean isEnabled) {
        keyStore.setEnabled(isEnabled, this);
        storekeyLabel.setEnabled(isEnabled);
        storekeyDec.setEnabled(isEnabled);
        aliasLabel.setEnabled(isEnabled);
        alias.setEnabled(isEnabled);
        passkeyLabel.setEnabled(isEnabled);
        passkeyDec.setEnabled(isEnabled);
    }

    public void setKeystoreCertInfo(KeystoreCertificateInfo info) {
        this.info = info;
    	initUI();
    }

    public KeystoreCertificateInfo getKeystoreCertInfo() {
    	if (dirty) {
    		updateKeystoreCertInfo();
    	}
        return info;
    }

    private void initUI() {
    	listener.setActive(false);
        String keyStorePath = info == null ? "" : info.getKeystoreLocation();
        keyStore.setStringValue(keyStorePath == null ? "" : keyStorePath);
        String storekeyValue = info == null ? "" : info.getKeystorePassword();
        storekey.setText(storekeyValue == null ? "" : storekeyValue);
        String aliasValue = info == null ? "" : info.getAlias();
        alias.setText(aliasValue == null ? "" : aliasValue);
        String passkeyValue = info == null ? "" : info.getKeyPassword();
        passkey.setText(passkeyValue == null ? "" : passkeyValue);
        listener.setActive(true);
    }

    void updateKeystoreCertInfo() {
    	info = new KeystoreCertificateInfo(keyStore.getStringValue(), alias.getText(), storekey.getText(), passkey.getText(), true);
    	dirty = false;
    }

	@Override
	public void updateUI() {
		this.dirty = true;
		if (updatable != null) {
			updatable.updateUI();
		}
	}

	public void setUpdatable(IUpdatableControl updatable) {
		this.updatable = updatable;
	}
}
