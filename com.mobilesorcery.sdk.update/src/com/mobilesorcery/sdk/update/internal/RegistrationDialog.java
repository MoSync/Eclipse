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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.ui.UIUtils;

public class RegistrationDialog extends Dialog {

	private Text nameText;
    private Text mailText;
    private Button mailinglistButton;
    
    private RegistrationInfo info = new RegistrationInfo();
	private boolean isStartedByUser;
	private Button autoStartup;

    public RegistrationDialog(Shell parentShell) {
        super(parentShell);
    }

    public Control createDialogArea(Composite parent) {        
    	getShell().setText(Messages.RegistrationDialog_Title);
        Composite main = (Composite) super.createDialogArea(parent);

        Label description = new Label(main, SWT.WRAP);
        description.setText(Messages.RegistrationDialog_Description);
        Label nameLabel = new Label(main, SWT.NONE);
        nameLabel.setText(Messages.RegistrationDialog_NameCaption);
        nameText = new Text(main, SWT.BORDER | SWT.SINGLE);
        nameText.setText(getInfo().name);
        nameText.setLayoutData(new GridData(UIUtils.getDefaultFieldSize(), SWT.DEFAULT));
        
        Label mailLabel = new Label(main, SWT.NONE);
        mailLabel.setText(Messages.RegistrationDialog_EmailCaption);
        mailText = new Text(main, SWT.BORDER | SWT.SINGLE);
        mailText.setText(getInfo().mail);
        mailText.setLayoutData(new GridData(UIUtils.getDefaultFieldSize(), SWT.DEFAULT));       
        
        mailinglistButton = new Button(main, SWT.CHECK);
        mailinglistButton.setText(Messages.RegistrationDialog_SubscriptionCaption);
        mailinglistButton.setSelection(getInfo().mailinglist);
        
        //if (!isStartedByUser) {
        // Should always show.
        	autoStartup = new Button(main, SWT.CHECK);
        	autoStartup.setSelection(CoreMoSyncPlugin.getDefault().getPreferenceStore().getBoolean(MoSyncTool.AUTO_UPDATE_PREF));
        	autoStartup.setText(Messages.RegistrationDialog_CheckUpdatesCaption);
        //}
        
        return main;
    }
    
    public void okPressed() {
    	info.name = nameText.getText();
    	info.mail = mailText.getText();
    	info.mailinglist = mailinglistButton.getSelection();
        
    	updateCheckOnStartup();
    	
        super.okPressed();
    }
    
    public void cancelPressed() {
        // If you check that box, you'll probably want to keep that setting
        // even if you cancel.
        updateCheckOnStartup();
        super.cancelPressed();
    }
    
    private void updateCheckOnStartup() {
        if (autoStartup != null) {
            CoreMoSyncPlugin.getDefault().getPreferenceStore().setValue(MoSyncTool.AUTO_UPDATE_PREF, autoStartup.getSelection());
        }        
    }
    
    public RegistrationInfo getInfo() {
    	return info;
    }
    
    public void setInfo(RegistrationInfo info) {
    	this.info = info;
    }

	public void setIsStartedByUser(boolean isStartedByUser) {
		this.isStartedByUser = isStartedByUser;
	}
}
