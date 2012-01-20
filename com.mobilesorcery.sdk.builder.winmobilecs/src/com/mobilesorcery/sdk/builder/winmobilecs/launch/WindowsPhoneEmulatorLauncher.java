package com.mobilesorcery.sdk.builder.winmobilecs.launch;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.mobilesorcery.sdk.builder.winmobilecs.WinMobileCSPackager;
import com.mobilesorcery.sdk.builder.winmobilecs.WinMobileCSPlugin;
import com.mobilesorcery.sdk.builder.winmobilecs.WindowsPhoneEmulator;
import com.mobilesorcery.sdk.builder.winmobilecs.ui.dialogs.ConfigureWindowsPhoneEmulatorDialog;
import com.mobilesorcery.sdk.core.BuildVariant;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IPackager;
import com.mobilesorcery.sdk.core.launch.AbstractEmulatorLauncher;
import com.mobilesorcery.sdk.core.launch.IEmulatorLauncher;

public class WindowsPhoneEmulatorLauncher extends AbstractEmulatorLauncher {

	public final static String SDK_ATTR = "iphone.sdk";

	private static final String ID = "com.mobilesorcery.sdk.builder.winmobilecs.launcher";

	public WindowsPhoneEmulatorLauncher() {
		super("Windows Phone Emulator");
	}

	@Override
	public int isLaunchable(ILaunchConfiguration launchConfiguration, String mode) {
		if (!Util.isWindows()) {
			return UNLAUNCHABLE;
		} else if (!isCorrectPackager(launchConfiguration)) {
			return IEmulatorLauncher.UNLAUNCHABLE;
		} if (shouldAskUserForLauncher(launchConfiguration, mode)) {
			return IEmulatorLauncher.REQUIRES_CONFIGURATION;
		} else if (!isCorrectlyInstalled()) {
			return IEmulatorLauncher.UNLAUNCHABLE;
		} else {
			return super.isLaunchable(launchConfiguration, mode);
		}
	}

	private boolean shouldAskUserForLauncher(ILaunchConfiguration launchConfiguration, String mode) {
		return isCorrectlyInstalled() && isAutoSelectLaunch(launchConfiguration, mode) && shouldAskUserForLauncher(WinMobileCSPackager.ID);
	}

	@Override
	public void launch(ILaunchConfiguration launchConfig, String mode,
			ILaunch launch, int emulatorId, IProgressMonitor monitor)
			throws CoreException {
		final File packageToInstall = getPackageToInstall(launchConfig, mode);
		WindowsPhoneEmulator.getDefault().run(packageToInstall);
	}

	@Override
	public IBuildVariant getVariant(ILaunchConfiguration launchConfig, String mode) throws CoreException {
		IBuildVariant prototype = super.getVariant(launchConfig, mode);
		BuildVariant modified = new BuildVariant(prototype);
		modified.setSpecifier(WinMobileCSPlugin.WP_EMULATOR_SPECIFIER, WinMobileCSPlugin.WP_EMULATOR_SPECIFIER);
		return modified;
	}

	@Override
	public IEmulatorLauncher configure(ILaunchConfiguration config, String mode) {
		Display d = PlatformUI.getWorkbench().getDisplay();

		final IEmulatorLauncher[] result = new IEmulatorLauncher[] { null };
		d.syncExec(new Runnable() {
			@Override
			public void run() {
				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				result[0] = showConfigureDialog(shell);
			}
		});
		return result[0];
	}

	protected IEmulatorLauncher showConfigureDialog(Shell shell) {
		ConfigureWindowsPhoneEmulatorDialog dialog = new ConfigureWindowsPhoneEmulatorDialog(shell);
		dialog.setIsAutomaticSelection(true);
		dialog.setNeedsConfig(false);
		dialog.open();
		return dialog.getSelectedLauncher();
	}

	@Override
	public int getLaunchType(IPackager packager) {
		return Util.equals(packager.getId(), WinMobileCSPackager.ID) && Util.isWindows() ? LAUNCH_TYPE_NATIVE : LAUNCH_TYPE_NONE;
	}

	protected boolean isCorrectlyInstalled() {
		return WindowsPhoneEmulator.getDefault().isValid();
	}
}
