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
import java.util.HashMap;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
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
import org.eclipse.swt.widgets.Button;
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
import com.mobilesorcery.sdk.ui.Note;
import com.mobilesorcery.sdk.ui.ProfileContentProvider;
import com.mobilesorcery.sdk.ui.ProfileLabelProvider;
import com.mobilesorcery.sdk.ui.UIUtils;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhone;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhoneTransport;
import com.mobilesorcery.sdk.ui.targetphone.TargetPhonePlugin;

public class EditDeviceListDialog extends Dialog {

    public class TargetDeviceLabelProvider extends LabelProvider {
        @Override
		public String getText(Object o) {
            ITargetPhone t = (ITargetPhone) o;
            ITargetPhoneTransport tt = t.getTransport();
            return MessageFormat.format("{0} [{1}]", t.getName(), tt.getDescription(""));
        }
    }

    private ComboViewer deviceList;
    private TreeViewer preferredProfile;
    private ITargetPhone initialTargetPhone;
    private boolean fixedDevice;
    private final HashMap<ITargetPhone, IProfile> pendingChanges = new HashMap<ITargetPhone, IProfile>();
	private Integer profileManagerType;

    public EditDeviceListDialog(Shell parentShell) {
        super(parentShell);
    }

    public void setInitialTargetPhone(ITargetPhone initialTargetPhone) {
        this.initialTargetPhone = initialTargetPhone;
    }

    @Override
	public Control createDialogArea(Composite parent) {
        getShell().setText("Select Preferred Profile");

        ITargetPhone initialTargetPhone = this.initialTargetPhone == null ? TargetPhonePlugin.getDefault().getCurrentlySelectedPhone()
                : this.initialTargetPhone;

        Composite main = (Composite) super.createDialogArea(parent);

        Composite contents = new Composite(main, SWT.NONE);
        contents.setLayout(new GridLayout(1, false));

        Label deviceLabel = new Label(main, SWT.NONE | SWT.WRAP);
        deviceLabel.setText("Target &Device:");

        if (!fixedDevice) {
            deviceList = new ComboViewer(contents, SWT.BORDER | SWT.READ_ONLY);
            deviceList.setContentProvider(new ArrayContentProvider());
            deviceList.setLabelProvider(new TargetDeviceLabelProvider());
            deviceList.addSelectionChangedListener(new ISelectionChangedListener() {
                @Override
				public void selectionChanged(SelectionChangedEvent event) {
                    updateFilter();
                    updateUI(null, false);
                }
            });

            deviceList.getCombo().setLayoutData(new GridData(UIUtils.getDefaultFieldSize(), SWT.DEFAULT));

            Link clear = new Link(contents, SWT.NONE);
            clear.setText("<a>Clear History</a>");
            clear.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
            clear.addListener(SWT.Selection, new Listener() {
                @Override
				public void handleEvent(Event event) {
                    clearDeviceList(getShell());
                    updateUI(null, true);
                }
            });
        }

        Label instructions = new Label(contents, SWT.NONE | SWT.WRAP);
        String instructionMsg = "Click to select new target profile";
        instructions.setText(instructionMsg);

        if (getProfileManagerType() == MoSyncTool.LEGACY_PROFILE_TYPE) {
        	Note note = new Note(contents, SWT.NONE);
        	note.setText("NOTE:\nThe selected project uses a (legacy) device based profile database.\nThis is why this list looks different than for platform based profile databases.");
        	note.setLayoutData(new GridData(UIUtils.getDefaultFieldSize(), SWT.DEFAULT));
        }

        preferredProfile = new TreeViewer(contents, SWT.SINGLE | SWT.BORDER);
        ProfileLabelProvider labelProvider = new ProfileLabelProvider(SWT.NONE);
        labelProvider.setTargetProfileProvider(new ITargetProfileProvider() {
            @Override
			public IProfile getTargetProfile() {
                return getCurrentPreferredProfile();
            }
        });

        preferredProfile.setLabelProvider(labelProvider);
        final ProfileContentProvider contentProvider = new ProfileContentProvider();
        preferredProfile.setContentProvider(contentProvider);
        preferredProfile.getControl().setLayoutData(new GridData(UIUtils.getDefaultFieldSize(), UIUtils.getDefaultListHeight()));

        preferredProfile.addDoubleClickListener(new IDoubleClickListener() {
            @Override
			public void doubleClick(DoubleClickEvent event) {
                selectNewProfile(event.getSelection());
                okPressed();
            }
        });

        preferredProfile.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
			public void selectionChanged(SelectionChangedEvent event) {
                selectNewProfile(event.getSelection());
            }
        });

        updateFilter();
        updateUI(initialTargetPhone, true);

        return contents;
    }

    private void selectNewProfile(ISelection selection) {
        Object element = ((IStructuredSelection) selection).getFirstElement();
        if (element instanceof IProfile) {
            IProfile profile = (IProfile) element;
            ITargetPhone currentTargetPhone = getSelectedTargetPhone();

            if (currentTargetPhone != null) {
                pendingChanges.put(currentTargetPhone, profile);
            }

            updateButtons();
            preferredProfile.refresh();
        }
    }


    /**
     * Sets a fixed device for this dialog (resulting in no visible device
     * selector).
     *
     * @param b
     */
    public void setFixedDevice(boolean fixedDevice) {
        this.fixedDevice = fixedDevice;
    }

    protected ITargetPhone getSelectedTargetPhone() {
        if (fixedDevice) {
            return initialTargetPhone;
        } else {
            ITargetPhone selectedPhone = (ITargetPhone) ((IStructuredSelection) deviceList.getSelection()).getFirstElement();
            return selectedPhone;
        }
    }

    protected void updateFilter() {
        IDeviceFilter emulatorFilter = new EmulatorDeviceFilter(EmulatorDeviceFilter.EXCLUDE_EMULATORS);
        ITargetPhone phone = getSelectedTargetPhone();
        IDeviceFilter targetPhoneAcceptedProfiles = phone == null ? null : phone.getTransport().getAcceptedProfiles();
        IDeviceFilter filter = targetPhoneAcceptedProfiles == null ? emulatorFilter : new CompositeDeviceFilter(new IDeviceFilter[] { emulatorFilter,
                targetPhoneAcceptedProfiles });
        preferredProfile.setInput(MoSyncTool.getDefault().getProfileManager(getProfileManagerType()).getVendors(filter));
        preferredProfile.setFilters(new ViewerFilter[] { new DeviceViewerFilter(filter) });
    }

    @Override
	public void createButtonsForButtonBar(Composite parent) {
        Button okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        updateButtons();
    }

    protected IProfile getCurrentPreferredProfile() {
        IProfile profile = null;
        ITargetPhone selectedPhone = getSelectedTargetPhone();
        if (selectedPhone != null) {
            profile = pendingChanges.get(selectedPhone);
            if (profile == null) {
                profile = selectedPhone.getPreferredProfile(getProfileManagerType());
            }
        }

        return profile;
    }

    private int getProfileManagerType() {
		if (profileManagerType == null) {
			profileManagerType = TargetPhonePlugin.getDefault().getCurrentProfileManagerType();
		}
		return profileManagerType;
	}

	protected void updateUI(ITargetPhone changeToTargetPhone, boolean reloadPhones) {
        if (!fixedDevice) {
            if (reloadPhones) {
                deviceList.setInput(TargetPhonePlugin.getDefault().getSelectedTargetPhoneHistory().toArray());
            }

            if (changeToTargetPhone != null) {
                deviceList.setSelection(new StructuredSelection(changeToTargetPhone), true);

            }
        }
        IProfile profile = getCurrentPreferredProfile();
        preferredProfile.refresh();
        preferredProfile.setSelection(profile == null ? new StructuredSelection() : new StructuredSelection(profile), true);
        updateButtons();
    }

    private void updateButtons() {
        Button okButton = getButton(IDialogConstants.OK_ID);
        if (okButton != null) {
            okButton.setEnabled(okButtonEnabled());
        }
    }

    private boolean okButtonEnabled() {
        IProfile profile = getCurrentPreferredProfile();
        return profile != null;
    }

    @Override
	public void okPressed() {
        if (okButtonEnabled()) {
            commitPendingChanges();
            super.okPressed();
        }
    }

    private void commitPendingChanges() {
        for (ITargetPhone phone : pendingChanges.keySet()) {
            IProfile newProfile = pendingChanges.get(phone);
            phone.setPreferredProfile(newProfile);
        }
    }

    protected void clearDeviceList(Shell parent) {
        if (MessageDialog.openConfirm(parent, "Are you sure?", "This will clear the list of target devices -- are you sure?")) {
            TargetPhonePlugin.getDefault().clearHistory();
            close();
        }

    }

}
