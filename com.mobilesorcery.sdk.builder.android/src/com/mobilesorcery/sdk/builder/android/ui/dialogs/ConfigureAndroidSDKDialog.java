package com.mobilesorcery.sdk.builder.android.ui.dialogs;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.mobilesorcery.sdk.builder.android.AndroidPackager;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IPackager;
import com.mobilesorcery.sdk.ui.launch.ConfigureNativeEmulatorDialog;

public class ConfigureAndroidSDKDialog extends ConfigureNativeEmulatorDialog {

	public ConfigureAndroidSDKDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected void configure() {
		PreferencesUtil.createPreferenceDialogOn(null,
				"com.mobilesorcery.sdk.builder.android.preferences.sdk",
				new String[] { "com.mobilesorcery.sdk.builder.android.preferences.sdk"}, null).open();
		// Ok, we'll try to launch afterwards
		setSelectedLauncher(getNativeLauncher());
	}

	@Override
	protected IPackager getPackager() {
		return CoreMoSyncPlugin.getDefault().getPackagerById(AndroidPackager.ID);
	}

	@Override
	protected String createMessageBody(boolean isAutomaticSelection,
			boolean needsConfig) {
		if (isAutomaticSelection) {
			if (needsConfig) {
				return "You have selected an Android device. Some features available for Android such as " +
					"Native UI and OpenGL are not supported by MoRE. However, you can run your application " +
					"in the Android Emulator which supports them. Would you like to configure your Android SDK now?";
			} else {
				return "You have selected an Android device. Some features available for Android such as Native UI " +
					"and OpenGL are not supported by MoRE. However, you can run your application in " +
					"the Android Emulator which supports them.";
				}
		} else {
			return "The Android SDK has not yet been configured. Would you like to configure your Android SDK now?";
		}
	}
}
