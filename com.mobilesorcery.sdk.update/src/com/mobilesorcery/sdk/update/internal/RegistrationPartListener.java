package com.mobilesorcery.sdk.update.internal;

import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IPerspectiveListener3;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IUpdater;

public class RegistrationPartListener implements IPartListener, IPerspectiveListener3 {

    private IViewPart view;
    private boolean reopenIntro;
    private boolean active;

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
        IPerspectiveDescriptor perspective = PlatformUI.getWorkbench().getPerspectiveRegistry().findPerspectiveWithId(
                RegistrationPerspectiveFactory.REGISTRATION_PERSPECTIVE_ID);
        if (perspective != null) {
            active = false;
            page.closePerspective(perspective, false, false);
            active = true;
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
        if (active) {
            updateReopenIntro(desc);
            if (RegistrationPerspectiveFactory.REGISTRATION_PERSPECTIVE_ID.equals(desc.getId())) {
                IViewPart registrationView = page.findView(RegistrationWebBrowserView.VIEW_ID);
                if (registrationView instanceof RegistrationWebBrowserView && !((RegistrationWebBrowserView) registrationView).isActive()) {
                    IUpdater updater = CoreMoSyncPlugin.getDefault().getUpdater();
                    if (updater != null) {
                        updater.register(false);
                    }
                }
            }
        }
    }

    public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor desc, String s) {
    }

    public void perspectiveClosed(IWorkbenchPage page, IPerspectiveDescriptor desc) {

    }

    public void perspectiveDeactivated(IWorkbenchPage page, IPerspectiveDescriptor desc) {

    }

    public void perspectiveOpened(IWorkbenchPage page, IPerspectiveDescriptor desc) {
        if (active) {
            updateReopenIntro(desc);
        }
    }

    private void updateReopenIntro(IPerspectiveDescriptor desc) {
        if (active) {
            if (!RegistrationPerspectiveFactory.REGISTRATION_PERSPECTIVE_ID.equals(desc.getId())) {
                // Someone changed perspectives.
                reopenIntro = false;
            }
        }

    }

    public void perspectiveSavedAs(IWorkbenchPage page, IPerspectiveDescriptor desc1, IPerspectiveDescriptor desc2) {
        // TODO Auto-generated method stub

    }

    public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor desc, IWorkbenchPartReference iworkbenchpartreference, String s) {
        // TODO Auto-generated method stub

    }

    public void setReopenIntro(boolean reopenIntro) {
        this.reopenIntro = reopenIntro;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

}
