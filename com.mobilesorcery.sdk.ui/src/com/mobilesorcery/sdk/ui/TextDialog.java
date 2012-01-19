package com.mobilesorcery.sdk.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class TextDialog extends Dialog {

	private String text;
	private String title;

	public TextDialog(Shell parentShell) {
		super(parentShell);
	}

	public void setText(String text) {
		this.text = text;
	}

	@Override
	public Control createDialogArea(Composite parent) {
		Composite main = (Composite) super.createDialogArea(parent);
		Text text = new Text(main, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
		text.setLayoutData(new GridData(2 * UIUtils.getDefaultFieldSize(), UIUtils.getDefaultListHeight()));
		text.setText(this.text);
		getShell().setText(title);
		return main;
	}

	@Override
	public void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	public void setTitle(String title) {
		this.title = title;
	}

}
