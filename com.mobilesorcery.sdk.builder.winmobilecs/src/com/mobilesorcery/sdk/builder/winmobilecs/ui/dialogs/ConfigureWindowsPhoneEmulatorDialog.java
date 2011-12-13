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
						"com.mobilesorcery.sdk.builder.winmobilecs.emulator.prefs",
						new String[] { "com.mobilesorcery.sdk.builder.winmobilecs.emulator.prefs" },
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
				return "You have selected a Windows Phone device. Some features available for Windows Phone such as Native UI and OpenGL are not supported by MoRE. "
						+ "If you install and configure the Windows Phone Emulator, MoSync can automatically launch it for you instead of MoRE.";
			} else {
				return "You have selected an Windows Phone device. Some features available for Windows Phone such as Native UI "
						+ "and OpenGL are not supported by MoRE. However, you can run your application in "
						+ "the Window Phone Emulator which supports them.";
			}
		} else {
			return "No Windows Phone Emulator has been configured. Would you like to configure it now?";
		}
	}

}
