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
package com.mobilesorcery.ui.perspective;

import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;

public class PerspectiveFactory implements IPerspectiveFactory {

    private IPageLayout layout;

    @Override
	public void createInitialLayout(IPageLayout layout) {
        this.layout = layout;
        addViews();
        addPerspectiveShortcuts();
        addActionSets();
    }

    private void addActionSets() {
    	layout.addActionSet(IDebugUIConstants.LAUNCH_ACTION_SET);
    	layout.addActionSet("org.eclipse.debug.ui.profileActionSet");
        layout.addActionSet("com.mobilesorcery.sdk.ui.targetphone");
    }

    private void addViews() {
        IFolderLayout bottom = layout.createFolder(
                "bottomRight", //NON-NLS-1
                IPageLayout.BOTTOM,
                0.75f,
                layout.getEditorArea());
        bottom.addView(IPageLayout.ID_PROBLEM_VIEW);
        bottom.addPlaceholder(IConsoleConstants.ID_CONSOLE_VIEW);

        IFolderLayout topLeft =
            layout.createFolder(
                "topLeft", //NON-NLS-1
                IPageLayout.LEFT,
                0.25f,
                layout.getEditorArea());
        //topLeft.addView(IPageLayout.ID_RES_NAV);
        topLeft.addView("org.eclipse.ui.navigator.ProjectExplorer");
        topLeft.addPlaceholder("com.mobilesorcery.sdk.testing.view");

        IFolderLayout topRight =
        	layout.createFolder("topRight", IPageLayout.RIGHT, 0.70f, layout.getEditorArea());

        topRight.addPlaceholder("com.mobilesorcery.sdk.profiles.ui.view");
        topRight.addPlaceholder("com.mobilesorcery.sdk.finalizer.ui.view");
        topRight.addView("org.eclipse.ui.views.ContentOutline");
    }

    private void addPerspectiveShortcuts() {
        layout.addPerspectiveShortcut("com.mobilesorcery.sdk.profiles.ui.view"); //NON-NLS-1
    }
}
