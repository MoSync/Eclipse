package com.mobilesorcery.sdk.ui;

import java.text.MessageFormat;
import java.util.ArrayList;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public class ProgressAndStepDialog extends ProgressMonitorDialog {

	/**
	 * The id of the "Wait in Background" button as well as
	 * the return code from this dialog if it's pressed.
	 */
	public static final int ID_RUN_IN_BKG = 1 << 16;

	private ArrayList<Image> icons = new ArrayList<Image>();
	private ArrayList<String> messages = new ArrayList<String>();

	private boolean waitInBackground = false;

	public ProgressAndStepDialog(Shell shell) {
		super(shell);
	}

	protected Control createMessageArea(Composite parent) {
		addMessages();

		for (int i = 0; i < icons.size(); i++) {
			Image icon = icons.get(i);
			String message = messages.get(i);
			Label iconLabel = new Label(parent, SWT.NONE);
			iconLabel.setAlignment(SWT.CENTER);
			iconLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false,
					false));
			if (icon != null) {
				iconLabel.setImage(icon);
			}
			Link messageLabel = new Link(parent, SWT.NONE);
			messageLabel.setLayoutData(new GridData(SWT.DEFAULT, SWT.TOP, true,
					false));
			messageLabel.setText(message);
			messageLabel.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					performAction(event.text);
				}
			});

			Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
			separator.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true,
					false, 2, 1));
		}

		// Important addition:
		super.createMessageArea(parent);

		return parent;
	}

	public Image getImage() {
		return PlatformUI.getWorkbench().getSharedImages()
				.getImage(ISharedImages.IMG_OBJS_INFO_TSK);
	}

	public void buttonPressed(int buttonId) {
		super.buttonPressed(buttonId);
		if (buttonId == ID_RUN_IN_BKG) {
			setReturnCode(ID_RUN_IN_BKG);
			waitInBackgroundPressed();
		}
	}
	
	protected void waitInBackgroundPressed() {
		waitInBackground = true;
		close();
	}
	
	protected boolean shouldWaitInBackground() {
		return waitInBackground;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, ID_RUN_IN_BKG, "Wait in Background", true);
		createCancelButton(parent);
	}
	
	protected void configureShell(final Shell shell) {
		super.configureShell(shell);
		// We don't want that wait cursor.
		shell.setCursor(arrowCursor);
	}

	protected void performAction(String actionId) {
	}

	protected void addMessages() {
		
	}

	protected void addMessage(Image icon, String message, Object... messageArguments) {
		this.icons.add(icon);
		this.messages.add(MessageFormat.format(message, messageArguments));
	}

}
