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
package com.mobilesorcery.sdk.profiles.ui.internal;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.mobilesorcery.sdk.profiles.IDeviceFilter;

public class SelectFilterTypeDialog extends Dialog {

    private IFilterProvider[] filterProviders;
    private IFilterProvider selectedFilterProvider;
    private IDeviceFilter filter;

    protected SelectFilterTypeDialog(Shell parentShell) {
        super(parentShell);
    }

    public void createButtonsForButtonBar(Composite parent) {
        Button ok = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        updateUI();
    }

    public Control createDialogArea(Composite parent) {
        getShell().setText(Messages.SelectFilterTypeDialog_SelectFilterType);
        
        Composite contents = new Composite(parent, SWT.NONE);
        contents.setLayout(new GridLayout(1, false));
     
        // TODO: Externalize
        filterProviders = new IFilterProvider[] {
            new ProfileFilterDialog(getShell()),
            new FeatureFilterDialog(getShell()),
            new ConstantFilterDialog(getShell())
        };
        
        final Button[] filterRadioButtons = new Button[filterProviders.length];
        
        for (int i = 0; i < filterProviders.length; i++) {
            filterRadioButtons[i] = new Button(contents, SWT.RADIO);
            filterRadioButtons[i].setText(filterProviders[i].getName());
            filterRadioButtons[i].setData(filterProviders[i]);
            filterRadioButtons[i].addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent e) {
                    widgetSelected(e);
                }

                public void widgetSelected(SelectionEvent e) {                    
                    selectedFilterProvider = (IFilterProvider) e.widget.getData();
                    updateUI();
                }
            });
        }
        
        return contents;
    }
    
    public void okPressed() {
        getShell().close();
        filter = selectedFilterProvider.getFilter();
        super.okPressed();
    }

    public IDeviceFilter getFilter() {
        return filter;
    }
    
    private void updateUI() {
        getButton(IDialogConstants.OK_ID).setEnabled(selectedFilterProvider != null);
    }

}
