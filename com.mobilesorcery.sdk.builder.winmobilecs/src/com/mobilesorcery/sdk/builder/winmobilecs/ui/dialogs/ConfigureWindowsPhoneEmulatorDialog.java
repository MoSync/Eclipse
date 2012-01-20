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
		throw new IllegalStateException("Cannot configure windows phone emulator -- missing from MOSYNCDIR/bin?");
	}

	@Override
	protected IPackager getPackager() {
		return CoreMoSyncPlugin.getDefault().getPackagerById(
				WinMobileCSPackager.ID);
	}

	@Override
	protected String createMessageBody(boolean isAutomaticSelection,
			boolean needsConfig) {
		if (!isAutomaticSelection || needsConfig) {
			throw new IllegalStateException("Internal error");
		}
		return "You have selected an Windows Phone device. Some features available for Windows Phone such as Native UI "
				+ "and OpenGL are not supported by MoRE. However, you can run your application in "
				+ "the Window Phone Emulator which supports them.";
	}

}
