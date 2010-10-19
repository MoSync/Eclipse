package com.mobilesorcery.sdk.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;


/**
 * <p>Creates a text field that will have a
 * greyed-out description text if empty and out of focus.</p>
 * <p>Use {@link #getText()} of this class
 * instead of the wrapped {@link org.eclipse.swt.widgets.Text#getText};
 * otherwise the description text will be returned</p>
 */
public class DescriptiveTextFieldListener implements Listener {
	private Text text;
	private boolean descriptionMode = false;
	private String description;
	private boolean hasFocus;

	public DescriptiveTextFieldListener(Text text, String description) {
		this.text = text;
		this.description = description;
		hasFocus = text.isFocusControl();
		descriptionMode = text.getText().length() == 0;
		enterNewState(hasFocus, true);
		text.addListener(SWT.FocusIn, this);
		text.addListener(SWT.FocusOut, this);
	}

	public void handleEvent(Event event) {
		if (event.type == SWT.FocusIn || event.type == SWT.FocusOut) {
			enterNewState(event.type == SWT.FocusIn, false);
		}
	}
	
	public void enterNewState(boolean hasFocus, boolean first) {
		boolean hadFocus = this.hasFocus;
		this.hasFocus = hasFocus;
		
		boolean gotFocus = hasFocus && !hadFocus;
		boolean lostFocus = !hasFocus && hadFocus;
		
		if (gotFocus && descriptionMode) {
			text.setText("");
			descriptionMode = false;
		}
		
		if (lostFocus || first) {
			descriptionMode = text.getText().length() == 0;
			if (descriptionMode) {
				text.setText(description);
			}
		}
	
		text.setForeground(text.getDisplay().getSystemColor(descriptionMode ? SWT.COLOR_DARK_GRAY : SWT.COLOR_BLACK));
	}
	
	public boolean isInDescriptionMode() {
		return descriptionMode;
	}
	
	public String getText() {
		return descriptionMode ? "" : text.getText();
	}
}