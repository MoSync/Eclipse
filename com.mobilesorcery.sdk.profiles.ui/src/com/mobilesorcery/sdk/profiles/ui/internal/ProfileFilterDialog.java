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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.filter.FeatureFilter;
import com.mobilesorcery.sdk.profiles.filter.ProfileFilter;
import com.mobilesorcery.sdk.profiles.ui.ProfileContentProvider;
import com.mobilesorcery.sdk.ui.ProfileLabelProvider;
import com.mobilesorcery.sdk.ui.UIUtils;

public class ProfileFilterDialog extends DeviceFilterDialog<ProfileFilter> {

    private ContainerCheckedTreeViewer profiles;
    private Button require;
    private Button disallow;

    public ProfileFilterDialog(Shell parentShell) {
        super(parentShell);
        setName(Messages.ProfileFilterDialog_VendorAltDevice);
        filter = new ProfileFilter();
        setFilter(filter);
    }

    @Override
	public Control createDialogArea(Composite parent) {
        getShell().setText(getName());
        Composite contents = new Composite(parent, SWT.NONE);
        contents.setLayout(new GridLayout(1, false));

        require = new Button(contents, SWT.RADIO);
        require.setText(Messages.ProfileFilterDialog_Require);
        require.setSelection(filter.getStyle() == ProfileFilter.REQUIRE);

        disallow = new Button(contents, SWT.RADIO);
        disallow.setText(Messages.ProfileFilterDialog_Disallow);
        require.setSelection(filter.getStyle() == ProfileFilter.DISALLOW);

        profiles = new ContainerCheckedTreeViewer(contents, SWT.BORDER);

        profiles.setLabelProvider(new ProfileLabelProvider(ProfileLabelProvider.NO_IMAGES));
        final ProfileContentProvider contentProvider = new ProfileContentProvider();
        profiles.setContentProvider(contentProvider);
        profiles.getControl().setLayoutData(new GridData(UIUtils.getDefaultFieldSize(), UIUtils.getDefaultListHeight()));

        profiles.setInput(MoSyncTool.getDefault().getProfileManager(MoSyncTool.LEGACY_PROFILE_TYPE).getVendors());

        profiles.setCheckedElements(filter.getProfiles());

        return contents;
    }

    @Override
	public void okPressed() {
        filter.clear();

        Object[] checkedElements = profiles.getCheckedElements();
        for (int i = 0; i < checkedElements.length; i++) {
            Object checkedElement = checkedElements[i];
            if (checkedElement instanceof IProfile) {
                IProfile profile = (IProfile) checkedElement;
                filter.setProfile(profile, true);
            }
        }

        filter.setStyle(require.getSelection() ? FeatureFilter.REQUIRE : FeatureFilter.DISALLOW);
        super.okPressed();
    }

}
