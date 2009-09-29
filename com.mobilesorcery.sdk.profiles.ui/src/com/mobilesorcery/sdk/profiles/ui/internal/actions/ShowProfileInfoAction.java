package com.mobilesorcery.sdk.profiles.ui.internal.actions;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.statushandlers.StatusManager;

import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.ui.Activator;

public class ShowProfileInfoAction extends Action {

	private ISelection selection;

	public ShowProfileInfoAction() {
		super(Messages.ShowProfileInfoAction_ShowProfileInfo, ImageDescriptor.createFromImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_INFO_TSK)));
	}

	public void setSelection(ISelection selection) {
		this.selection = selection;
	}

	public void run() {
        if (selection instanceof IStructuredSelection) {
            Object maybeProfile = ((IStructuredSelection) selection).getFirstElement();
            if (maybeProfile instanceof IProfile) {
                IProfile profile = (IProfile) maybeProfile;
                IPath profileInfoFile = MoSyncTool.getDefault().getProfileInfoFile(profile);
                IFileStore fileStore = EFS.getLocalFileSystem().getStore(profileInfoFile);
                IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
             
                try {
                    IDE.openEditorOnFileStore(page, fileStore);
                } catch (Exception e) {
                	Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.ShowProfileInfoAction_ProfilerViewerError, e);
                	StatusManager.getManager().handle(status, StatusManager.SHOW);
                }
            }
        }
    }
}
