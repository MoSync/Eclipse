package com.mobilesorcery.sdk.html5.ui;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IconAndMessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public class JSODDTimeoutDialog extends IconAndMessageDialog {

	private final static String DONT_SHOW = "dont.show.timeout.dialog";
	
	private Button rememberMyChoice;

	private ReloadVirtualMachine vm;

	public JSODDTimeoutDialog(Shell shell) {
		super(shell);
	}

	public static void openIfNecessary(Shell shell, ReloadVirtualMachine vm) {
		boolean show = !Html5Plugin.getDefault().getPreferenceStore().getBoolean(DONT_SHOW);
		if (show) {
			JSODDTimeoutDialog dialog = new JSODDTimeoutDialog(shell);
			dialog.setVM(vm);
			dialog.open();
		}
	}
	
	public void setVM(ReloadVirtualMachine vm) {
		this.vm = vm;
	}

	public Control createDialogArea(Composite parent) {
		Composite contents = (Composite) super.createDialogArea(parent);
		contents.setLayout(new GridLayout(2, false));
		getShell().setText("Timeout");
		message = MessageFormat.format("A timeout occurred.\n\nThe device being debugged (at {0}) seems to have been disconnected.\nThe debugging session has been terminated.\n\n", vm.getRemoteAddr());
		createMessageArea(contents);
		
		// Remember my choice?
		Label spacer = new Label(contents, SWT.NONE);
		rememberMyChoice = new Button(contents, SWT.CHECK);
		rememberMyChoice.setText("Do not show this message again");
		rememberMyChoice.setSelection(false);
		return contents;
	}

	public void createButtonsForButtonBar(Composite parent) {
		createButton(parent, Dialog.OK, IDialogConstants.OK_LABEL, true);
	}

	public void buttonPressed(int buttonId) {
		if (buttonId != -1) {
			Html5Plugin.getDefault().getPreferenceStore().setValue(DONT_SHOW, rememberMyChoice.getSelection());
		}
		setReturnCode(buttonId);
		close();
	}
	
	@Override
	protected Image getImage() {
		return getWarningImage();
	}

}
