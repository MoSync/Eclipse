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
