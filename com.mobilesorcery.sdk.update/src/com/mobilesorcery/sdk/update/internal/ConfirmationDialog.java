/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.update.internal;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.ui.UIUtils;

public class ConfirmationDialog extends Dialog {

    private Text confirmationCodeText;
    private String confirmationCode;
	private boolean isStartedByUser;
	private Button autoStartup;

    public ConfirmationDialog(Shell parentShell) {
        super(parentShell);
    }

    public Control createDialogArea(Composite parent) {
    	getShell().setText(Messages.ConfirmationDialog_Title);
        Composite main = (Composite) super.createDialogArea(parent);
        
        Label desc = new Label(main, SWT.NONE);
        desc.setText(Messages.ConfirmationDialog_Description);
        
        confirmationCodeText = new Text(main, SWT.BORDER | SWT.SINGLE);
        confirmationCodeText.setLayoutData(new GridData(UIUtils.getDefaultFieldSize(), SWT.DEFAULT));
        
        //if (!isStartedByUser) {
        	autoStartup = new Button(main, SWT.CHECK);
        	autoStartup.setSelection(CoreMoSyncPlugin.getDefault().getPreferenceStore().getBoolean(MoSyncTool.AUTO_UPDATE_PREF));
        	autoStartup.setText(Messages.ConfirmationDialog_CheckUpdates);
        //}
        
        Link resend = new Link(main, SWT.NONE);
        resend.setText(Messages.ConfirmationDialog_Resend);
        resend.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				ResendConfirmationCodeAction resend = new ResendConfirmationCodeAction();
				resend.run();
			}        	
        });
                
        
        return main;
    }
    
    public void okPressed() {
        confirmationCode = confirmationCodeText.getText();
        updateCheckOnStartup();
        super.okPressed();
    }
    
    public void cancelPressed() {
        updateCheckOnStartup();
        super.cancelPressed();
    }
    
    private void updateCheckOnStartup() {
        if (autoStartup != null) {
            CoreMoSyncPlugin.getDefault().getPreferenceStore().setValue(MoSyncTool.AUTO_UPDATE_PREF, autoStartup.getSelection());
        }        
    }
    
    public String getConfirmationCode() {
        return confirmationCode;
    }

	public void setIsStartedByUser(boolean isStartedByUser) {
		this.isStartedByUser = isStartedByUser;
	}
}
