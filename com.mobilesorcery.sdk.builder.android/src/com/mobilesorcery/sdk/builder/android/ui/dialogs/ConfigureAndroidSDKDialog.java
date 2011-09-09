package com.mobilesorcery.sdk.builder.android.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IconAndMessageDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.mobilesorcery.sdk.builder.android.Activator;
import com.mobilesorcery.sdk.ui.UpdateListener;
import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;

public class ConfigureAndroidSDKDialog extends IconAndMessageDialog implements IUpdatableControl {

	public static final int FALLBACK_ID = 0xff01;

	public static final int CONFIGURE_ID = 0xff02;

	private Button dontAskAgain;

	private boolean showFallback;

	public ConfigureAndroidSDKDialog(Shell parentShell) {
		super(parentShell);
		message = "You have selected an Android device. Some features available for Android such as " +
				"Native UI and OpenGL are not supported by MoRE. However, you can run your application " +
				"in the Android Emulator which supports them. Would you like to configure your Android SDK now?";
	}

	@Override
	public Control createDialogArea(Composite parent) {
		getShell().setText("Configure Android SDK");
		Composite contents = (Composite) super.createDialogArea(parent);
		contents.setLayout(new GridLayout(2, false));
		createMessageArea(contents);
		if (showFallback) {
			Label spacer = new Label(contents, SWT.NONE);
			dontAskAgain = new Button(contents, SWT.CHECK);
			dontAskAgain.setText("Do not ask this again");
			dontAskAgain.addListener(SWT.Selection, new UpdateListener(this));
		}
		return contents;
	}

    @Override
	public void createButtonsForButtonBar(Composite parent) {
    	createButton(parent, CONFIGURE_ID, "Configure Android SDK", false);
    	createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    	if (showFallback) {
    		createButton(parent, FALLBACK_ID, "Run in MoRE", true);
    	}
	}

    @Override
    public void buttonPressed(int buttonId) {
    	setReturnCode(buttonId);
    	if (CONFIGURE_ID == buttonId) {
    		close();
    		int result = PreferencesUtil.createPreferenceDialogOn(null,
    				"com.mobilesorcery.sdk.builder.android.preferences.sdk",
    				new String[] { "com.mobilesorcery.sdk.builder.android.preferences.sdk"}, null).open();
    	} else if (FALLBACK_ID == buttonId) {
			Activator.getDefault().setUseFallback(dontAskAgain.getSelection());
    		close();
    	} else {
    		super.buttonPressed(buttonId);
    	}
    }

	public void setShowFallback(boolean showFallback) {
		this.showFallback = showFallback;
	}

	@Override
	protected Image getImage() {
		return getQuestionImage();
	}

	@Override
	public void updateUI() {
		if (showFallback) {
			getButton(CONFIGURE_ID).setEnabled(!dontAskAgain.getSelection());
		}
	}
}
