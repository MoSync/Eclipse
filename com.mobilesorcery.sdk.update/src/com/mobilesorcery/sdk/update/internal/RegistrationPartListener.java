package com.mobilesorcery.sdk.update.internal;

import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IPerspectiveListener3;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;

public class RegistrationPartListener implements IPartListener, IPerspectiveListener3 {

    private IViewPart view;
    private boolean reopenIntro;

    public RegistrationPartListener(IViewPart view, boolean reopenIntro) {
        this.view = view;
        this.reopenIntro = reopenIntro;
    }

    public void partActivated(IWorkbenchPart part) {
    }

    public void partBroughtToTop(IWorkbenchPart part) {
    }

    public void partClosed(IWorkbenchPart part) {
        if (part == this.view) {
            view.getSite().getPage().removePartListener(this);
            view.getSite().getWorkbenchWindow().removePerspectiveListener(this);
            closeRegistrationPerspective();
        }
    }
    
    public void closeRegistrationPerspective() {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IPerspectiveDescriptor perspective = PlatformUI.getWorkbench().getPerspectiveRegistry().findPerspectiveWithId(DefaultUpdater2.REGISTRATION_PERSPECTIVE_ID);
        if (perspective != null) {
            page.closePerspective(perspective, false, false);
        }

        if (reopenIntro) {
            PlatformUI.getWorkbench().getIntroManager().showIntro(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), false);
        }
    }



    public void partDeactivated(IWorkbenchPart part) {
    }

    public void partOpened(IWorkbenchPart part) {
    }

    public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor desc) {
        updateReopenIntro(desc);
    }

    public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor desc, String s) {
    }

    public void perspectiveClosed(IWorkbenchPage page, IPerspectiveDescriptor desc) {
        
    }

    public void perspectiveDeactivated(IWorkbenchPage page, IPerspectiveDescriptor desc) {
        
    }

    public void perspectiveOpened(IWorkbenchPage page, IPerspectiveDescriptor desc) {
        updateReopenIntro(desc);
    }

    private void updateReopenIntro(IPerspectiveDescriptor desc) {
        if (!DefaultUpdater2.REGISTRATION_PERSPECTIVE_ID.equals(desc.getId())) {
            // Someone changed perspectives.
            reopenIntro = false;
        }
        
    }

    public void perspectiveSavedAs(IWorkbenchPage page, IPerspectiveDescriptor desc1, IPerspectiveDescriptor desc2) {
        // TODO Auto-generated method stub
        
    }

    public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor desc,
            IWorkbenchPartReference iworkbenchpartreference, String s) {
        // TODO Auto-generated method stub
        
    }

    public void setReopenIntro(boolean reopenIntro) {
        this.reopenIntro = reopenIntro;
    }

}
