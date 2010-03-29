package com.mobilesorcery.sdk.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class PasswordDialog extends Dialog {

    private final class InternalListener implements Listener {
        public void handleEvent(Event event) {
            updateUI();
        }
    }

    private Text pwdText;
    private String password;
    private Button rememberPwd;
    private boolean shouldRememberPassword;
    private Button showInClearText;
    public PasswordDialog(Shell parentShell) {
        super(parentShell);
    }
    
    public Control createDialogArea(Composite parent) {
        Composite main = (Composite) super.createDialogArea(parent);
        main.setLayout(new GridLayout(2, false));
        
        InternalListener listener = new InternalListener();
        
        Label pwdLabel = new Label(main, SWT.NONE);
        pwdLabel.setText("Password:");
        
        pwdText = new Text(main, SWT.SINGLE | SWT.BORDER);
        pwdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        pwdText.setText(password == null ? "" : password);
        
        Label spacer1 = new Label(main, SWT.NONE);
        showInClearText = new Button(main, SWT.CHECK);
        showInClearText.setText("&Show password");
        
        Label spacer2 = new Label(main, SWT.NONE);
        rememberPwd = new Button(main, SWT.CHECK);
        rememberPwd.setText("&Remember this password");
     
        updateUI();

        pwdText.addListener(SWT.Modify, listener);
        showInClearText.addListener(SWT.Selection, listener);
        
        getShell().setText("Password");
        return main;
    }
    
    protected void updateUI() {
        char echoChar = showInClearText.getSelection() ? '\0' : '\u2022';
        pwdText.setEchoChar(echoChar);
        rememberPwd.setEnabled(pwdText.getText().length() > 0);
    }

    public void okPressed() {
        password = pwdText.getText();
        shouldRememberPassword = password.length() > 0 && rememberPwd.getSelection();
        super.okPressed();
    }
    
    public void setInitialPassword(String password) {
        this.password = password;
    }
    
    public String getPassword() {
        return password;
    }
    
    public boolean shouldRememberPassword() {
        return shouldRememberPassword;
    }

}
