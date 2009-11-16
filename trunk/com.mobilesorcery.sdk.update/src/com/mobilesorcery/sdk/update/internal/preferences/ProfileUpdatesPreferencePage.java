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
package com.mobilesorcery.sdk.update.internal.preferences;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.update.UpdateManager;

public class ProfileUpdatesPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private Text emailText;
	private Button clearSettings;

	public ProfileUpdatesPreferencePage() {
        super(Messages.ProfileUpdatesPreferencePage_Title, CoreMoSyncPlugin.getImageDescriptor("/icons/mosyncproject.png"), GRID); //$NON-NLS-2$
        
        IPreferenceStore store = CoreMoSyncPlugin.getDefault().getPreferenceStore();
        setPreferenceStore(store);
    }
    
	protected void createFieldEditors() {
		BooleanFieldEditor autoUpdate = new BooleanFieldEditor(MoSyncTool.AUTO_UPDATE_PREF, Messages.ProfileUpdatesPreferencePage_AutoUpdate, getFieldEditorParent());
    	addField(autoUpdate);
    	
    	Composite registration = new Composite(getFieldEditorParent(), SWT.NONE);
    	registration.setLayout(new GridLayout(2, false));
    	registration.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    	
    	Label reg = new Label(registration, SWT.NONE);
    	reg.setText(Messages.ProfileUpdatesPreferencePage_RegistrationDescription);
    	reg.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    	clearSettings = new Button(registration, SWT.PUSH);
    	clearSettings.setText(Messages.ProfileUpdatesPreferencePage_ClearRegistrationInfo);
    	clearSettings.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));

    	emailText = new Text(registration, SWT.READ_ONLY | SWT.SINGLE);
    	GridData emailData = new GridData(GridData.FILL_HORIZONTAL);
    	emailData.horizontalSpan = 2;
    	emailText.setLayoutData(emailData);
    	
    	updateUI();
    	
    	
    	clearSettings.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				boolean result = MessageDialog.openQuestion(getShell(), Messages.ProfileUpdatesPreferencePage_AreYouSureTitle, Messages.ProfileUpdatesPreferencePage_AreYouSureMessage);
				
				if (result) {
					UpdateManager.getDefault().clearRegistrationInfo();
					updateUI();
				}
			}    		
    	});
	}

	private void updateUI() {
		String email = MoSyncTool.getDefault().getProperty(MoSyncTool.EMAIL_PROP);
		if (email == null || email.equals("")) { //$NON-NLS-1$
			emailText.setText(Messages.ProfileUpdatesPreferencePage_NotRegistered);
		} else {
			emailText.setText(email);
		}
		
		emailText.setEnabled(email != null && !email.equals("")); //$NON-NLS-1$
    	clearSettings.setEnabled(email != null && !email.equals("")); //$NON-NLS-1$
	}

	public void init(IWorkbench window) {
	}

}
