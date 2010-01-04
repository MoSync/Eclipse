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
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.mobilesorcery.sdk.core.IUpdater;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.update.MosyncUpdatePlugin;

public class DefaultUpdater implements IUpdater {

	public class UpdateRunnable implements Runnable {
		
		private UpdateProfilesAction update;

		public UpdateRunnable(UpdateProfilesAction update) {
			this.update = update;
		}
		
		public void run() {
			boolean done = false;
			while (!done) {
				done = tryToShowDialog(update);
				try {
					if (!done) {
						// Quite acceptable, should not take many iterations to get here
						Thread.sleep(1000);
					}
				} catch (InterruptedException e) {
					// Ignore.
				}
			}
		}

	}

	public DefaultUpdater() {
	}

	/**
	 * This update method must be called from a UI thread, or a potential ui thread.
	 */
	public void update(boolean isStartedByUser) {
		IPreferenceStore prefStore = MosyncUpdatePlugin.getDefault()
				.getPreferenceStore();
		prefStore.setDefault(MoSyncTool.AUTO_UPDATE_PREF, true);


		UpdateProfilesAction update = new UpdateProfilesAction();
		update.setIsStartedByUser(false); // Ignore some UI, add some other
		
		if (MoSyncTool.getDefault().isValid()
				&& prefStore.getBoolean(MoSyncTool.AUTO_UPDATE_PREF)) {

			Thread updateThread = new Thread(new UpdateRunnable(update), "Profile update");
			updateThread.setDaemon(true);
			updateThread.start();
		}
	}


	private boolean tryToShowDialog(UpdateProfilesAction update) {
		// Since the updater is called very early, we cannot
		// be sure we have a workbench yet. Hence this strange concurrent implementation.
		IWorkbench wb = PlatformUI.getWorkbench();
		if (wb == null) {
			return false;
		}

		try {
			update.run();
		} finally {
			return true;
		}

	}

}
