package com.mobilesorcery.sdk.update.internal;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class RegistrationPerspectiveFactory implements IPerspectiveFactory {

    public void createInitialLayout(IPageLayout layout) {
        layout.setEditorAreaVisible(true);
    }

}
