package com.mobilesorcery.sdk.builder.iphoneos.ui.dialogs;

import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IconAndMessageDialog;
import org.eclipse.jface.util.Policy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;

import com.mobilesorcery.sdk.builder.iphoneos.Activator;
import com.mobilesorcery.sdk.ui.UpdateListener;
import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;

public class ConfigureXcodeDialog extends IconAndMessageDialog implements IUpdatableControl {

	public static final int FALLBACK_ID = 0xff01;

	public static final int DOWNLOAD_SDK_ID = 0xff02;

	private Button dontAskAgain;

	private boolean showFallback;

	public ConfigureXcodeDialog(Shell parentShell) {
		super(parentShell);
		message = "You have selected an iOS device. Some features available for iOS such as Native UI and OpenGL are not supported by MoRE. " +
				"If you install and configure the iOS SDK, MoSync can automatically launch it for you instead of MoRE.";
	}

	@Override
	public Control createDialogArea(Composite parent) {
		getShell().setText("iOS Simulator not found");
		Composite contents = (Composite) super.createDialogArea(parent);
		contents.setLayout(new GridLayout(2, false));
		createMessageArea(contents);
		if (showFallback) {
			Label spacer = new Label(contents, SWT.NONE);
			dontAskAgain = new Button(contents, SWT.CHECK);
			dontAskAgain.setText("Do not show this message again");
			dontAskAgain.addListener(SWT.Selection, new UpdateListener(this));
		}
		return contents;
	}

    @Override
	public void createButtonsForButtonBar(Composite parent) {
    	createButton(parent, DOWNLOAD_SDK_ID, "Go To iPhone SDK Download", false);
    	if (showFallback) {
        	createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    		createButton(parent, FALLBACK_ID, "Run in MoRE", true);
    	} else {
        	createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    	}
	}

    @Override
    public void buttonPressed(int buttonId) {
    	setReturnCode(buttonId);
    	if (FALLBACK_ID == buttonId) {
			Activator.getDefault().setUseFallback(dontAskAgain.getSelection());
    		close();
    	} else if (DOWNLOAD_SDK_ID == buttonId) {
    		close();
    		IWebBrowser externalBrowser;
			try {
				externalBrowser = PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser();
    		externalBrowser.openURL(new URL("http://developer.apple.com/devcenter/ios"));
			} catch (Exception e) {
				Policy.getStatusHandler().show(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not open browser", e), "Could not open browser");
			}
    	} else {
    		super.buttonPressed(buttonId);
    	}
    }

	public void setShowFallback(boolean showFallback) {
		this.showFallback = showFallback;
	}

	@Override
	protected Image getImage() {
		return showFallback ? getQuestionImage() : getInfoImage();
	}

	@Override
	public void updateUI() {
		if (showFallback) {
			getButton(DOWNLOAD_SDK_ID).setEnabled(!dontAskAgain.getSelection());
		}
	}
}
