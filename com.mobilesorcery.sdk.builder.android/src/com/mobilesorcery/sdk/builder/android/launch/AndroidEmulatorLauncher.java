package com.mobilesorcery.sdk.builder.android.launch;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import com.mobilesorcery.sdk.builder.android.Activator;
import com.mobilesorcery.sdk.builder.android.AndroidPackager;
import com.mobilesorcery.sdk.core.CollectingLineHandler;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildState;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IPackager;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.launch.AbstractEmulatorLauncher;
import com.mobilesorcery.sdk.internal.launch.EmulatorLaunchConfigurationDelegate;
import com.mobilesorcery.sdk.profiles.IProfile;

public class AndroidEmulatorLauncher extends AbstractEmulatorLauncher {

	public static final String AVD_NAME = "avd";

	public AndroidEmulatorLauncher() {
		super("Android Emulator");
	}

	@Override
	public void assertLaunchable(ILaunchConfiguration launchConfig, String mode) throws CoreException {
		assertCorrectPackager(launchConfig, AndroidPackager.ID, "The Android Emulator requires the target profile to be an Android device");
		super.assertLaunchable(launchConfig, mode);
	}
	
	@Override
	public void launch(ILaunchConfiguration launchConfig, String mode,
			ILaunch launch, int emulatorId, IProgressMonitor monitor)
			throws CoreException {
		ADB adb = ADB.getExternal();
		adb.assertValid();	
		
		List<String> emulators = adb.listEmulators(true);
		if (emulators.size() == 0) {
			Emulator emulator = Emulator.getExternal();
			emulator.assertValid();
			String avd = launchConfig.getAttribute(AVD_NAME, "");
			CollectingLineHandler handler = emulator.start(avd, true);
			emulators = awaitEmulatorStarted(adb, handler, 2, TimeUnit.MINUTES);
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

	private List<String> awaitEmulatorStarted(ADB adb, CollectingLineHandler emulatorProcess, int timeout, TimeUnit unit) throws CoreException {
		// Hm... better ways to do this?
		long now = System.currentTimeMillis();
		long timeoutInMs = TimeUnit.MILLISECONDS.convert(timeout, unit);
		boolean wasStopped = emulatorProcess.isStopped();
		while (!wasStopped && System.currentTimeMillis() - now < timeoutInMs) {
			List<String> emulators = adb.listEmulators(false);
			if (emulators.size() == 1) {
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

}
