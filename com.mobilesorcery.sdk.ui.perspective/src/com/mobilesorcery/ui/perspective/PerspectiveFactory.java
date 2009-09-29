package com.mobilesorcery.ui.perspective;

import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;

public class PerspectiveFactory implements IPerspectiveFactory {

    private IPageLayout layout;

    public void createInitialLayout(IPageLayout layout) {
        this.layout = layout;
        addViews();
        addPerspectiveShortcuts();
        addActionSets();
    }

    private void addActionSets() {
        layout.addActionSet(IDebugUIConstants.LAUNCH_ACTION_SET);
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
        
        topRight.addView("com.mobilesorcery.sdk.profiles.ui.view");
        topRight.addView("com.mobilesorcery.sdk.finalizer.ui.view");
    }

    private void addPerspectiveShortcuts() {
        layout.addPerspectiveShortcut("com.mobilesorcery.sdk.profiles.ui.view"); //NON-NLS-1
    }
}
