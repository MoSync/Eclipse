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
package com.mobilesorcery.sdk.ui.targetphone.internal;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.ITargetProfileProvider;
import com.mobilesorcery.sdk.profiles.filter.CompositeDeviceFilter;
import com.mobilesorcery.sdk.profiles.filter.EmulatorDeviceFilter;
import com.mobilesorcery.sdk.profiles.ui.DeviceViewerFilter;
import com.mobilesorcery.sdk.profiles.ui.ProfileContentProvider;
import com.mobilesorcery.sdk.profiles.ui.ProfileLabelProvider;
import com.mobilesorcery.sdk.ui.UIUtils;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhoneTransport;
import com.mobilesorcery.sdk.ui.targetphone.TargetPhonePlugin;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhone;
import com.mobilesorcery.sdk.ui.targetphone.internal.bt.BTTargetPhone;

public class EditDeviceListDialog extends Dialog {

	public class TargetDeviceLabelProvider extends LabelProvider {
		public String getText(Object o) {
			ITargetPhone t = (ITargetPhone) o; 
			ITargetPhoneTransport tt = t.getTransport();
			return MessageFormat.format("{0} [{1}]", t.getName(), tt.getDescription(""));
		}
	}

	private ComboViewer deviceList;
	private TreeViewer preferredProfile;
	private ITargetPhone initialTargetPhone;

	protected EditDeviceListDialog(Shell parentShell) {
		super(parentShell);
	}

	public void setInitialTargetPhone(ITargetPhone initialTargetPhone) {
		this.initialTargetPhone = initialTargetPhone;
	}
	
    public Control createDialogArea(Composite parent) {
        getShell().setText("Select Preferred Profile");
        
        ITargetPhone initialTargetPhone = this.initialTargetPhone == null ? TargetPhonePlugin.getDefault().getCurrentlySelectedPhone() : this.initialTargetPhone;
        
        Composite main = (Composite) super.createDialogArea(parent);
        
        Composite contents = new Composite(main, SWT.NONE);
        contents.setLayout(new GridLayout(1, false));
        
        deviceList = new ComboViewer(contents, SWT.BORDER | SWT.READ_ONLY);
        deviceList.setContentProvider(new ArrayContentProvider());
        deviceList.setLabelProvider(new TargetDeviceLabelProvider());
        deviceList.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateFilter();
				updateUI(null, false);
			}
		});
        
        deviceList.getCombo().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        Link clear = new Link(contents, SWT.NONE);
        clear.setText("<a>Clear History</a>");
        clear.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
        clear.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				clearDeviceList(getShell());
				updateUI(null, true);
			}        	
        });
        
        Label instructions = new Label(contents, SWT.NONE | SWT.WRAP);
        instructions.setText("Double-click to select new target device");
        
        preferredProfile = new TreeViewer(contents, SWT.SINGLE | SWT.BORDER);
        ProfileLabelProvider labelProvider = new ProfileLabelProvider(SWT.NONE);
        labelProvider.setTargetProfilerProvider(new ITargetProfileProvider() {
			public IProfile getTargetProfile() {
				return getCurrentPreferredProfile(deviceList);
			}
		});
        
        preferredProfile.setLabelProvider(labelProvider);
        final ProfileContentProvider contentProvider = new ProfileContentProvider();
        preferredProfile.setContentProvider(contentProvider);
        preferredProfile.getControl().setLayoutData(new GridData(UIUtils.getDefaultFieldSize(), UIUtils.getDefaultListHeight()));
        
        preferredProfile.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				Object element = ((IStructuredSelection) event.getSelection()).getFirstElement();
				if (element instanceof IProfile) {
					IProfile profile = (IProfile) element;
					ITargetPhone currentTargetPhone = getSelectedTargetPhone();

					if (currentTargetPhone!= null) {
						currentTargetPhone.setPreferredProfile(profile);
					}
					preferredProfile.refresh();
				}
			}
		});

        updateUI(initialTargetPhone, true);
        
        return contents;
    }

    protected ITargetPhone getSelectedTargetPhone() {
		IStructuredSelection selection = (IStructuredSelection) deviceList.getSelection();
		ITargetPhone selectedTargetPhone = (ITargetPhone) selection.getFirstElement();
		return selectedTargetPhone;
	}

	protected void updateFilter() {
        IDeviceFilter emulatorFilter = new EmulatorDeviceFilter(EmulatorDeviceFilter.EXCLUDE_EMULATORS);
        ITargetPhone phone = getSelectedTargetPhone();
        IDeviceFilter targetPhoneAcceptedProfiles = phone == null ? null : phone.getTransport().getAcceptedProfiles();
        IDeviceFilter filter = targetPhoneAcceptedProfiles == null ? emulatorFilter : new CompositeDeviceFilter(new IDeviceFilter[] { emulatorFilter, targetPhoneAcceptedProfiles });
        preferredProfile.setInput(MoSyncTool.getDefault().getVendors(filter));
        preferredProfile.setFilters(new ViewerFilter[] { new DeviceViewerFilter(filter) });
	}

	public void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }

	protected IProfile getCurrentPreferredProfile(ComboViewer deviceList) {
		ITargetPhone selectedPhone = (ITargetPhone) ((IStructuredSelection) deviceList.getSelection()).getFirstElement();
		IProfile profile = null;
		if (selectedPhone != null) {
			profile = selectedPhone.getPreferredProfile();
		}

		return profile;
	}

	protected void updateUI(ITargetPhone changeToTargetPhone, boolean reloadPhones) {
		if (reloadPhones) {
			deviceList.setInput(TargetPhonePlugin.getDefault().getSelectedTargetPhoneHistory().toArray());
		}
		
        if (changeToTargetPhone != null) {
        	deviceList.setSelection(new StructuredSelection(changeToTargetPhone), true);
        	
        }    
		IProfile profile = getCurrentPreferredProfile(deviceList);
		preferredProfile.refresh();
		preferredProfile.setSelection(profile == null ? new StructuredSelection() : new StructuredSelection(profile), true);
	}

	protected void clearDeviceList(Shell parent) {
		if (MessageDialog.openConfirm(parent, "Are you sure?", "This will clear the list of target devices -- are you sure?")) {
			TargetPhonePlugin.getDefault().clearHistory();
			close();
		}
		
	}

}
