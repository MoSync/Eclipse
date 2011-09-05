package com.mobilesorcery.sdk.ui;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Text;

/**
 * <p>A password field (with approriate decorations)
 * created from a 'vanilla' text field.</p>
 * <p>The difference from the standard <code>SWT.PASSWORD</code>
 * flag is that clients can turn on/off hiding the password,
 * either via an API or via the text field decorations that
 * will be attached</p>
 */
public class PasswordTextFieldDecorator implements SelectionListener {

    private final Text text;
    private boolean hidden = true;
    private final ControlDecoration decoration;

    /**
     * Creates a password text field.
     * @param text The text field to decorate
     */
    public PasswordTextFieldDecorator(Text text) {
        this.text = text;
        decoration = new ControlDecoration(text, SWT.LEFT | SWT.BOTTOM);
        hidePassword(true);
        setEnabled(true);
        decoration.addSelectionListener(this);
    }

    public void hidePassword(boolean hidden) {
        this.hidden = hidden;
        text.setEchoChar(hidden ? '\u2022' : '\0');
        updateDecoration();
    }

    public void setEnabled(boolean enabled) {
        text.setEnabled(enabled);
        if (enabled) {
            decoration.show();
        } else {
            decoration.hide();
        }
    }

    private void updateDecoration() {
        decoration.setDescriptionText(hidden ? "Show password" : "Hide password");
        decoration.setImage(MosyncUIPlugin.getDefault().getImageRegistry().get(hidden ? MosyncUIPlugin.PASSWORD_SHOW : MosyncUIPlugin.PASSWORD_HIDE));
    }

    @Override
	public void widgetDefaultSelected(SelectionEvent event) {
        widgetSelected(event);
    }

    @Override
	public void widgetSelected(SelectionEvent event) {
        hidePassword(!hidden);
    }
}
