package com.mobilesorcery.sdk.builder.android.launch;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.mobilesorcery.sdk.builder.android.Activator;
import com.mobilesorcery.sdk.builder.android.AndroidPackager;
import com.mobilesorcery.sdk.builder.android.ui.dialogs.ConfigureAndroidSDKDialog;
import com.mobilesorcery.sdk.core.CollectingLineHandler;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IPackager;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.launch.AbstractEmulatorLauncher;
import com.mobilesorcery.sdk.core.launch.IEmulatorLauncher;
import com.mobilesorcery.sdk.internal.launch.EmulatorLaunchConfigurationDelegate;

public class AndroidEmulatorLauncher extends AbstractEmulatorLauncher {

	public static final String AVD_NAME = "avd";

	public static final String AUTO_SELECT_AVD = "avd.auto.select";

	public static final String ID = "com.mobilesorcery.sdk.builder.android.launcher";

	public AndroidEmulatorLauncher() {
		super("Android Emulator");
	}

	@Override
	public int isLaunchable(ILaunchConfiguration launchConfiguration, String mode) {
		if (!isCorrectPackager(launchConfiguration)) {
			return IEmulatorLauncher.UNLAUNCHABLE;
		} else if (askUserForLauncher(launchConfiguration, mode)) {
			return IEmulatorLauncher.REQUIRES_CONFIGURATION;
		} else if (!isCorrectlyInstalled()) {
			IEmulatorLauncher preferredLauncher = CoreMoSyncPlugin.getDefault().getPreferredLauncher(AndroidPackager.ID);
			boolean useOtherLauncher = !askUserForLauncher(AndroidPackager.ID) && !Util.equals(preferredLauncher.getId(), ID);
			return isAutoSelectLaunch(launchConfiguration, mode) && useOtherLauncher ?
					IEmulatorLauncher.UNLAUNCHABLE :
					IEmulatorLauncher.REQUIRES_CONFIGURATION;
		} else if (hasNoAVDs()) {
			return IEmulatorLauncher.REQUIRES_CONFIGURATION;
		} else {
			return super.isLaunchable(launchConfiguration, mode);
		}
	}

	private boolean askUserForLauncher(ILaunchConfiguration launchConfiguration, String mode) {
		return isCorrectlyInstalled() && isAutoSelectLaunch(launchConfiguration, mode) && askUserForLauncher(AndroidPackager.ID);
	}

	protected boolean isCorrectlyInstalled() {
		return ADB.getExternal().isValid() && Emulator.getExternal().isValid() && Android.getExternal().isValid();
	}

	protected boolean hasNoAVDs() {
		try {
			return Android.getExternal().listAVDs().isEmpty();
		} catch (CoreException e) {
			CoreMoSyncPlugin.getDefault().log(e);
			return true;
		}
	}

	@Override
	public void launch(ILaunchConfiguration launchConfig, String mode,
			ILaunch launch, int emulatorId, IProgressMonitor monitor)
			throws CoreException {
		ADB adb = ADB.getExternal();
		Android android = Android.getExternal();
		android.refresh();

		List<String> emulators = adb.listEmulators(true);
		if (emulators.size() == 0) {
			Emulator emulator = Emulator.getExternal();
			emulator.assertValid();
			String avd = getAVD(android, launchConfig);
			if (Util.isEmpty(avd)) {
				throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, MessageFormat.format("No AVD specified (modify your launch configuration).", avd)));
			}
			if (!android.hasAVD(avd)) {
				throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, MessageFormat.format("No AVD found with name {0} (modify your launch configuration).", avd)));
			}
			CollectingLineHandler handler = emulator.start(avd, true);
			emulators = awaitEmulatorStarted(adb, handler, 2, TimeUnit.MINUTES);
			//startLogCat(adb);
		} else if (emulators.size() > 1) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "This launcher only supports launching if exactly one Android emulator is started"));
		}

		IProject project = EmulatorLaunchConfigurationDelegate.getProject(launchConfig);

    	File packageToInstall = getPackageToInstall(launchConfig, mode);
    	if (packageToInstall != null) {
    		String serialNumberOfDevice = emulators.get(0);
    		adb.install(packageToInstall, serialNumberOfDevice);
    		adb.launch(Activator.getAndroidComponentName(MoSyncProject.create(project)), serialNumberOfDevice);
        } else {
        	throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Project not built or build failed"));
        }
	}

	private String getAVD(Android android, ILaunchConfiguration launchConfig) throws CoreException {
		boolean autoSelect = launchConfig.getAttribute(AUTO_SELECT_AVD, true);
		if (autoSelect) {
			List<AVD> avds = android.listAVDs();
			AVD bestMatch = null;
			for (AVD avd : avds) {
				int apiLevel = avd.getAPILevel();
				if (bestMatch == null || apiLevel > bestMatch.getAPILevel()) {
					bestMatch = avd;
				}
			}
			return bestMatch == null ? null : bestMatch.getName();
		} else {
			return launchConfig.getAttribute(AVD_NAME, "");
		}
	}

	private void startLogCat(ADB adb) throws CoreException {
		adb.startLogCat();
	}

	private List<String> awaitEmulatorStarted(ADB adb, CollectingLineHandler emulatorProcess, int timeout, TimeUnit unit) throws CoreException {
		// Hm... better ways to do this? Ok, here is an adb command to wait.
		// However, the problem is still the boot time!
		long now = System.currentTimeMillis();
		long timeoutInMs = TimeUnit.MILLISECONDS.convert(timeout, unit);
		boolean wasStopped = emulatorProcess.isStopped();
		while (!wasStopped && System.currentTimeMillis() - now < timeoutInMs) {
			List<String> emulators = adb.listEmulators(false);
			if (emulators.size() == 1) {
				adb.awaitBoot(emulators.get(0), TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES));
				return emulators;
			}
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				break;
			}
			wasStopped = emulatorProcess.isStopped();
		}

		if (!emulatorProcess.isStopped()) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Timeout occurred -- could not connect to Android Emulator"));
		} else {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not launch Android Emulator; wrong arguments?"));
		}
	}

	@Override
	public void setDefaultAttributes(ILaunchConfigurationWorkingCopy wc) {
		wc.setAttribute(AUTO_SELECT_AVD, true);
	}

	@Override
	public IEmulatorLauncher configure(ILaunchConfiguration config, String mode) {
		Display d = PlatformUI.getWorkbench().getDisplay();
		// So any changes to the AVDs will be propagated.
		Android.getExternal().refresh();
		// If we are not auto-select, don't fallback to MoRe.
		final boolean isAutomaticLaunch = isAutoSelectLaunch(config, mode);
		// And if we are supposed to ask the user, we do not really need to configure anything.
		final boolean needsConfig = !askUserForLauncher(config, mode);

		final IEmulatorLauncher[] result = new IEmulatorLauncher[] { null };
		d.syncExec(new Runnable() {
			@Override
			public void run() {
				// OK, figure out after 2.6 release where to really put this ui stuff!
				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				if (needsConfig && isCorrectlyInstalled() && hasNoAVDs()) {
					result[0] = showAutoCreateAVD(shell);
				} else {
					result[0] = showConfigureDialog(shell, isAutomaticLaunch, needsConfig);
				}
			}
		});
		return result[0];
	}

	protected IEmulatorLauncher showAutoCreateAVD(Shell shell) {
		boolean goAhead = MessageDialog.openConfirm(shell, "No AVD installed",
				"No AVD (Android Virtual Device) has been installed into your Android SDK.\n" +
				"Would you like to launch the Android AVD manager to help you create an AVD?");
		if (goAhead) {
			try {
				Android.getExternal().launchUI(true);
			} catch (CoreException e) {
				CoreMoSyncPlugin.getDefault().log(e);
			}
		}
		return null; // Cancel execution
	}

	protected IEmulatorLauncher showConfigureDialog(Shell shell, boolean showFallbackAlternative, boolean needsConfig) {
		ConfigureAndroidSDKDialog dialog = new ConfigureAndroidSDKDialog(shell);
		dialog.setIsAutomaticSelection(showFallbackAlternative);
		dialog.setNeedsConfig(needsConfig);
		dialog.open();
		return dialog.getSelectedLauncher();
	}

	@Override
	public int getLaunchType(IPackager packager) {
		return Util.equals(packager.getId(), AndroidPackager.ID) ? LAUNCH_TYPE_NATIVE : LAUNCH_TYPE_NONE;
	}


}
