package com.mobilesorcery.sdk.ui.internal;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.internal.properties.MoSyncProjectPropertyPage;

public class LegacyProfileViewOpener implements PropertyChangeListener {

	private static final String LEGACY_PROFILE_INHIBIT_AUTO_OPEN_PREF = "legacy.profile.inhibit.auto.open";

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		final MoSyncProject[] project = new MoSyncProject[1];

		if (event.getPropertyName() == MoSyncProject.PROFILE_MANAGER_TYPE_KEY) {
			project[0] = (MoSyncProject) event.getSource();
		}

		if (event.getPropertyName() == MosyncUIPlugin.CURRENT_PROJECT_CHANGED || project[0] != null) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					IWorkbenchWindow wWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
					IPerspectiveDescriptor perspective = wWindow.getActivePage().getPerspective();
					boolean introShowing = PlatformUI.getWorkbench().getIntroManager().getIntro() != null;

					if (perspective == null || !Util.equals(perspective.getId(), "com.mobilesorcery.ui.perspective") || introShowing) {
						// We are only interested if we are in the mosync perspective; the best option
						// in 99.99% of the cases.
						return;
					}

					MoSyncProject currentProject = project[0];
					if (currentProject == null) {
						currentProject = MosyncUIPlugin.getDefault().getCurrentlySelectedProject(wWindow);
					}
					if (currentProject != null && currentProject.getProfileManagerType() == MoSyncTool.LEGACY_PROFILE_TYPE) {
						try {
					        wWindow.getActivePage().showView("com.mobilesorcery.sdk.finalizer.ui.view");
							wWindow.getActivePage().showView("com.mobilesorcery.sdk.profiles.ui.view");
						} catch (PartInitException e) {
							CoreMoSyncPlugin.getDefault().log(e);
						}
						setActive(false);
						MosyncUIPlugin.getDefault().removeListener(LegacyProfileViewOpener.this);
					}
				}
			});
		}
	}

	public boolean isActive() {
		return !MosyncUIPlugin.getDefault().getPreferenceStore().getBoolean(LEGACY_PROFILE_INHIBIT_AUTO_OPEN_PREF);
	}

	public void setActive(boolean active) {
		MosyncUIPlugin.getDefault().getPreferenceStore().setValue(LEGACY_PROFILE_INHIBIT_AUTO_OPEN_PREF, !active);
	}

}
