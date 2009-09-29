package com.mobilesorcery.sdk.update.internal;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.PlatformUI;

import com.mobilesorcery.sdk.core.IUpdater;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.update.MosyncUpdatePlugin;

public class DefaultUpdater implements IUpdater {

	public DefaultUpdater() {
	}

	public void update(boolean isStartedByUser) {
		IPreferenceStore prefStore = MosyncUpdatePlugin.getDefault().getPreferenceStore();
		prefStore.setDefault(MoSyncTool.AUTO_UPDATE_PREF, true);

		if (MoSyncTool.getDefault().isValid()
				&& prefStore.getBoolean(MoSyncTool.AUTO_UPDATE_PREF)) {
			UpdateProfilesAction update = new UpdateProfilesAction();
			update.init(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
			update.setIsStartedByUser(isStartedByUser); // Ignore some UI, add some other.
			update.run();
		}
	}

}
