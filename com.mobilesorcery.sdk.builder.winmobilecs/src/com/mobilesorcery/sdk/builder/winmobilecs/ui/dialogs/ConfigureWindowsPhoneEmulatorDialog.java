package com.mobilesorcery.sdk.builder.winmobilecs.ui.dialogs;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.mobilesorcery.sdk.builder.winmobilecs.WinMobileCSPackager;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IPackager;
import com.mobilesorcery.sdk.ui.launch.ConfigureNativeEmulatorDialog;

public class ConfigureWindowsPhoneEmulatorDialog extends
		ConfigureNativeEmulatorDialog {

	public ConfigureWindowsPhoneEmulatorDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected void configure() {
		PreferencesUtil
				.createPreferenceDialogOn(
						null,
						"com.mobilesorcery.sdk.builder.winmobilecs.preferences.msbuild",
						new String[] { "com.mobilesorcery.sdk.builder.winmobilecs.preferences.msbuild" },
						null).open();
		// Ok, we'll try to launch afterwards
		setSelectedLauncher(getNativeLauncher());
	}

	@Override
	protected IPackager getPackager() {
		return CoreMoSyncPlugin.getDefault().getPackagerById(
				WinMobileCSPackager.ID);
	}

	@Override
	protected String createMessageBody(boolean isAutomaticSelection,
			boolean needsConfig) {
		if (isAutomaticSelection) {
			if (needsConfig) {
				return "You have selected a Windows Phone device. Some features available for Windows Phone such as "
						+ "Native UI and OpenGL are not supported by MoRE. However, you can run your application "
						+ "in the Windows Phone Emulator which supports them.\n"
						+ "To be able to run on the Windows Phone emulator, you need to provide the location of your Visual Studio for Windows Phone. Would you like to configure the location of your Visual Studio for Windows Phone installation now?";
			} else {
				return "You have selected an Windows Phone device. Some features available for Windows Phone such as Native UI "
						+ "and OpenGL are not supported by MoRE. However, you can run your application in "
						+ "the Window Phone Emulator which supports them.";
			}
		} else {
			return "To be able to run on the Windows Phone emulator, you need to provide the location of your Visual Studio for Windows Phone. Would you like to configure the location of your Visual Studio for Windows Phone installation now?";
		}

	}

}
