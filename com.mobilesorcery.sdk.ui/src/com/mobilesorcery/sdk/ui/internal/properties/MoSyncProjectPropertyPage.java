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
package com.mobilesorcery.sdk.ui.internal.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.ui.MoSyncPropertyPage;
import com.mobilesorcery.sdk.ui.PlatformSelectionComposite;

public class MoSyncProjectPropertyPage extends MoSyncPropertyPage {

    public MoSyncProjectPropertyPage() {
		super(false);
	}

	@Override
	protected Control createContents(Composite parent) {
        Composite main = new Composite(parent, SWT.NONE);
        main.setLayout(new GridLayout(2, false));

        Label currentProfile = new Label(main, SWT.NONE);
        currentProfile.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        final MoSyncProject project = getProject();
        IProfile profile = project.getTargetProfile();

        if (profile == null) {
            currentProfile.setText("Current Target Profile: <none>");
        } else {
            currentProfile.setText("Current target profile: " + profile);
        }

        if (PlatformUI.getWorkbench().getViewRegistry().find("com.mobilesorcery.sdk.profiles.ui.view") != null) {
            Link showProfilesView = new Link(main, SWT.NONE);
            showProfilesView.setText("<a>Show &Profiles</a>");
            showProfilesView.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, true, false));
            showProfilesView.addListener(SWT.Selection, new Listener() {
                @Override
				public void handleEvent(Event event) {
            		PlatformSelectionComposite psc = new PlatformSelectionComposite(getShell(), SWT.SEARCH | SWT.BACKGROUND);
					psc.setProject(project);
					psc.show(SWT.NONE);
                    MoSyncProjectPropertyPage.this.performCancel();
                }
            });
        }

        return main;
    }

}
