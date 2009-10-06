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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.mobilesorcery.sdk.profiles.IDeviceFilter;

public abstract class DeviceFilterDialog<T extends IDeviceFilter> extends Dialog implements IFilterProvider {

    private String name;
    protected T filter;
    
    public DeviceFilterDialog(Shell shell) {
        super(shell);        
    }
    
    public T getFilter() {
        int result = open();
        return result == IDialogConstants.OK_ID ? filter : null; 
    }
    
    public void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    public void setFilter(T filter) {
        this.filter = filter;
    }
    
    protected void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }

}
