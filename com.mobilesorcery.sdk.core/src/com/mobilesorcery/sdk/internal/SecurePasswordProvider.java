package com.mobilesorcery.sdk.internal;

import java.security.GeneralSecurityException;
import java.text.MessageFormat;

import javax.crypto.spec.PBEKeySpec;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.preference.IPreferenceStore;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IProvider;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;

// Consider refactoring this into two classes.
public class SecurePasswordProvider implements IProvider<PBEKeySpec, String> {

	private static final String SECURE_ROOT_NODE = "mosync.com";

	private static final String LOCAL_KEYRING_MASTER_KEY = "master.passkey";

	private static final String USE_ECLIPSE_SECURE_PREFS = "use.eclipse.secure.prefs";

	private static final String INSECURE_MASTER_KEY = "insecure.master.key";

	@Override
	public PBEKeySpec get(String key) {
		try {
			if (usesEclipseSecureStorage()) {
					String masterKey = SecurePreferencesFactory.getDefault().node(SECURE_ROOT_NODE).get(LOCAL_KEYRING_MASTER_KEY, "");
					if (Util.isEmpty(masterKey)) {
						masterKey = createMasterKey();
						SecurePreferencesFactory.getDefault().node(SECURE_ROOT_NODE).put(LOCAL_KEYRING_MASTER_KEY, masterKey, true);
					}
					return new PBEKeySpec(masterKey.toCharArray());
			} else {
				// Do not encrypt
				return null;
			}
		} catch (Exception e) {
			CoreMoSyncPlugin.getDefault().log(e);
			return null;
		}

	}

	public boolean usesEclipseSecureStorage() {
		IPreferenceStore prefs = CoreMoSyncPlugin.getDefault().getPreferenceStore();
		return prefs.getBoolean(USE_ECLIPSE_SECURE_PREFS);
	}

	public void doUseEclipseSecureStorage(boolean useEclipseSecureStorage) throws CoreException {
		boolean changed = useEclipseSecureStorage != usesEclipseSecureStorage();
		if (changed) {
			clearMasterPassword();
			IPreferenceStore prefs = CoreMoSyncPlugin.getDefault().getPreferenceStore();
			prefs.setValue(USE_ECLIPSE_SECURE_PREFS, useEclipseSecureStorage);
			resetMasterPassword();
		}
	}

	private void clearMasterPassword() {
		CoreMoSyncPlugin.getDefault().getLog().log(new Status(IStatus.INFO, CoreMoSyncPlugin.PLUGIN_ID, "Resetting (MoSync) master password."));
		// Reset the password, but only of the currently used password provider type.
		if (usesEclipseSecureStorage()) {
			SecurePreferencesFactory.getDefault().node(SECURE_ROOT_NODE).remove(LOCAL_KEYRING_MASTER_KEY);
		} else {
			CoreMoSyncPlugin.getDefault().getPreferenceStore().setToDefault(INSECURE_MASTER_KEY);
		}
	}

	private void resetMasterPassword() throws CoreException {
		MultiStatus result = new MultiStatus(CoreMoSyncPlugin.PLUGIN_ID, IStatus.OK, "Resetting master password failed for some projects.", null);
		resetMasterPassword(ResourcesPlugin.getWorkspace().getRoot(), result);
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			resetMasterPassword(project, result);
		}

		if (result.getChildren().length > 0) {
			CoreMoSyncPlugin.getDefault().getLog().log(result);
			throw new CoreException(result);
		}
	}

	private void resetMasterPassword(IResource resource, MultiStatus status) {
		String resourceName = "";
		try {
			switch (resource.getType()) {
			case IResource.ROOT:
				CoreMoSyncPlugin.getDefault().getSecureProperties().resetMasterPassword(this);
				resourceName = "workspace";
				break;
			case IResource.PROJECT:
				IProject project = (IProject) resource;
				if (project.isOpen()) {
					MoSyncProject mosyncProject = MoSyncProject.create(project);
					mosyncProject.getSecurePropertyOwner().resetMasterPassword(this);
				}
				resourceName = project.getName();
				break;
			}
		} catch (Exception e) {
			status.add(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, MessageFormat.format("Could not reset master password for: {0}", resourceName), e));
		}
	}

	private String createMasterKey() throws GeneralSecurityException, StorageException {
		String masterKey = SecureProperties.generateRandomKey();
		return masterKey;
	}

}
