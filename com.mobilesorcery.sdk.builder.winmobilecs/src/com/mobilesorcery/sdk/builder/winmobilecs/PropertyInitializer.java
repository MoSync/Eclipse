package com.mobilesorcery.sdk.builder.winmobilecs;

import java.util.UUID;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IPropertyInitializerDelegate;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.MoSyncProject;

public class PropertyInitializer extends AbstractPreferenceInitializer
		implements IPropertyInitializerDelegate {

	private static final String PREFIX = "winmobilecs:"; //$NON-NLS-1$

	public static final String GUID = PREFIX + "guid";

	@Override
	public String getDefaultValue(IPropertyOwner p, String key) {
		if (p instanceof MoSyncProject && key.startsWith(GUID)) {
			UUID uuid = CoreMoSyncPlugin.getDefault().generateUUID();
			String value = uuid.toString();
			p.initProperty(GUID, value);
			((MoSyncProject) p).updateProjectSpec();
			return value;
		}
		return null;
	}

	@Override
	public void initializeDefaultPreferences() {
		WinMobileCSPlugin.getDefault().getPreferenceStore().setDefault(WinMobileCSPlugin.ONLY_GENERATE_MS_BUILD_PROJECT, true);
	}

}