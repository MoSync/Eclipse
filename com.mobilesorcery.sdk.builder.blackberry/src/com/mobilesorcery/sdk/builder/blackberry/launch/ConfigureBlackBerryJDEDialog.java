package com.mobilesorcery.sdk.builder.blackberry.launch;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.mobilesorcery.sdk.builder.blackberry.BlackBerryPackager;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IPackager;
import com.mobilesorcery.sdk.ui.launch.ConfigureNativeEmulatorDialog;

public class ConfigureBlackBerryJDEDialog extends ConfigureNativeEmulatorDialog {

	public ConfigureBlackBerryJDEDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected void configure() {
		PreferencesUtil
				.createPreferenceDialogOn(
						null,
						"com.mobilesorcery.sdk.builder.blackberry.simulator.preferences",
						new String[] {
								"com.mobilesorcery.sdk.builder.blackberry.jde.preferences",
								"com.mobilesorcery.sdk.builder.blackberry.simulator.preferences" },
						null).open();
		// Ok, we'll try to launch afterwards
		setSelectedLauncher(getNativeLauncher());
	}

	@Override
	protected IPackager getPackager() {
		return CoreMoSyncPlugin.getDefault().getPackagerById(
				BlackBerryPackager.ID);
	}

	@Override
	protected String createMessageBody(boolean isAutomaticSelection,
			boolean needsConfig) {
		if (isAutomaticSelection) {
			if (needsConfig) {
				return "You have selected a BlackBerry device. Some features available for BlackBerry "
						+ "are not supported by MoRE. However, you can run your application "
						+ "in the BlackBerry Simulator which supports them. Would you like to configure your "
						+ "BlackBerry JDEs/Simulators now?";
			} else {
				return "You have selected a BlackBerry device. Some features available for BlackBerry"
						+ "are not supported by MoRE. However, you can run your application in "
						+ "the BlackBerry Simulator which supports them.";
			}
		} else {
			return "No BlackBerry JDE/Simulator has been configured. Would you like to configure your BlackBerry JDEs/Simulators now?";
		}
	}
}
