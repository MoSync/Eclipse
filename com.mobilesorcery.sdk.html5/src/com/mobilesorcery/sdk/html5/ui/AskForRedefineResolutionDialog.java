package com.mobilesorcery.sdk.html5.ui;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.Dialog;
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
import com.mobilesorcery.sdk.html5.debug.RedefineException;
import com.mobilesorcery.sdk.html5.debug.RedefinitionResult;
import com.mobilesorcery.sdk.ui.UpdateListener;

public class AskForRedefineResolutionDialog extends IconAndMessageDialog {

	private RedefineException error;
	private Button rememberMyChoice;

	public AskForRedefineResolutionDialog(Shell parentShell) {
		super(parentShell);
	}
	
	public void setRedefineException(RedefineException error) {
		this.error = error;
	}
		
	public Control createDialogArea(Composite parent) {
		Composite contents = (Composite) super.createDialogArea(parent);
		String errorMessage = error.getMessage();
		contents.setLayout(new GridLayout(2, false));
		message = MessageFormat.format("Some code changes cannot be hot swapped into a running JavaScript On-Device Debug client. It is safe to continue running the application, but you may notice discrepancies when debugging this application.\n\nReason: {0}", errorMessage);
		createMessageArea(contents);
		
		// Remember my choice?
		Label spacer = new Label(contents, SWT.NONE);
		rememberMyChoice = new Button(contents, SWT.CHECK);
		rememberMyChoice.setText("Remember my choice");
		rememberMyChoice.setSelection(false);
		return contents;
	}
	
	public void createButtonsForButtonBar(Composite parent) {
		createButton(parent, RedefinitionResult.TERMINATE, "Terminate", false);
		createButton(parent, RedefinitionResult.RELOAD, "Reload", false);
		createButton(parent, RedefinitionResult.CONTINUE, "Continue", true);
	}
	
	public void buttonPressed(int buttonId) {
		if (rememberMyChoice.getSelection() && buttonId != -1) {
			Html5Plugin.getDefault().setReloadStrategy(buttonId);
		}
	}

	@Override
	protected Image getImage() {
		return getQuestionImage();
	}
	
	public static int open(Shell shell, RedefineException e) {
		AskForRedefineResolutionDialog dialog = new AskForRedefineResolutionDialog(shell);
		dialog.setRedefineException(e);
		int result = dialog.open();
		if (result == -1) {
			result = RedefinitionResult.CONTINUE;
		}
		return result;
	}

}
