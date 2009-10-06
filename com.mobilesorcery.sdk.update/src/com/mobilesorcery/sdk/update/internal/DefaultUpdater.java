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
