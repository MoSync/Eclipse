package com.mobilesorcery.sdk.builder.android.launch;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.cdt.core.IBinaryParser.IBinaryObject;
import org.eclipse.cdt.debug.core.cdi.ICDISession;
import org.eclipse.cdt.internal.core.model.CModelManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.mobilesorcery.sdk.builder.android.Activator;
import com.mobilesorcery.sdk.builder.android.AndroidPackager;
import com.mobilesorcery.sdk.builder.android.PropertyInitializer;
import com.mobilesorcery.sdk.builder.android.launch.ADB.ProcessKiller;
import com.mobilesorcery.sdk.builder.android.launch.Emulator.IAndroidEmulatorProcess;
import com.mobilesorcery.sdk.builder.android.ui.dialogs.ConfigureAndroidSDKDialog;
import com.mobilesorcery.sdk.core.AbstractPackager;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.ILaunchConstants;
import com.mobilesorcery.sdk.core.IPackager;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.launch.AbstractEmulatorLauncher;
import com.mobilesorcery.sdk.core.launch.IEmulatorLauncher;
import com.mobilesorcery.sdk.internal.debug.MoSyncCDebugTarget;
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
		} else if (shouldAskUserForLauncher(launchConfiguration, mode)) {
			return IEmulatorLauncher.REQUIRES_CONFIGURATION;
		} else if (!isCorrectlyInstalled()) {
			IEmulatorLauncher preferredLauncher = CoreMoSyncPlugin.getDefault().getPreferredLauncher(AndroidPackager.ID);
			boolean useOtherLauncher = !shouldAskUserForLauncher(AndroidPackager.ID) && !Util.equals(preferredLauncher.getId(), ID);
			return isAutoSelectLaunch(launchConfiguration, mode) && useOtherLauncher ?
					IEmulatorLauncher.UNLAUNCHABLE :
					IEmulatorLauncher.REQUIRES_CONFIGURATION;
		} else if (hasNoAVDs()) {
			return IEmulatorLauncher.REQUIRES_CONFIGURATION;
		} else {
			return super.isLaunchable(launchConfiguration, mode);
		}
	}

	private boolean shouldAskUserForLauncher(ILaunchConfiguration launchConfiguration, String mode) {
		return isCorrectlyInstalled() && isAutoSelectLaunch(launchConfiguration, mode) && shouldAskUserForLauncher(AndroidPackager.ID);
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

	// Small hack to get odd up and running!
	private void launchOnDevice(ADB adb, ILaunchConfiguration launchConfig, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		String serialNumberOfDevice = launchConfig.getAttribute("serialno", "none");
		MoSyncProject project = MoSyncProject.create(EmulatorLaunchConfigurationDelegate.getProject(launchConfig));
		
		String arch = adb.matchAbi(serialNumberOfDevice, ADB.ABIS);
		
		IBuildVariant variant = EmulatorLaunchConfigurationDelegate.getVariant(launchConfig, mode);
		File executable = project.getBuildState(variant).getBuildResult().getBuildResult().get(IBuildResult.MAIN).get(0);
		
		boolean debug = AbstractPackager.shouldUseDebugRuntimes(project, variant);
		File binary = new File(AndroidPackager.computeNativeBuildResult(project, variant, arch, debug).getParentFile(), "app_process");
		AndroidNDKDebugger dbg = new AndroidNDKDebugger();
		dbg.setSerialNumber(serialNumberOfDevice);
    	ICDISession targetSession = dbg.createSession(launch, executable, new NullProgressMonitor());
    	
    	IFile[] binaryFiles = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(new Path(binary.getAbsolutePath()));
    	IFile binaryFile = null;
    	for (int i = 0; binaryFile == null && i < binaryFiles.length; i++) {
    		if (binaryFiles[i].getProject().equals(project.getWrappedProject())) {
    			binaryFile = binaryFiles[i];
    		}
    	}
    	IBinaryObject binaryObject = (IBinaryObject) CModelManager.getDefault().createBinaryFile(binaryFile);
    	IDebugTarget debugTarget = MoSyncCDebugTarget.newDebugTarget(launch, project.getWrappedProject(),
    			targetSession.getTargets()[0], launch.getLaunchConfiguration().getName(),
    			null, binaryObject, true, false, null, true);
	}
	
	@Override
	public void launch(ILaunchConfiguration launchConfig, String mode,
			ILaunch launch, int emulatorId, IProgressMonitor monitor)
			throws CoreException {
		ADB adb = ADB.getExternal();
		if (launchConfig.getAttribute(ILaunchConstants.ON_DEVICE, false)) {
			launchOnDevice(adb, launchConfig, mode, launch, monitor);
			return;
		}
		Android android = Android.getExternal();
		android.refresh();
		Emulator emulator = Emulator.getExternal();
		emulator.assertValid();

		String avd = getAVD(android, launchConfig);
		if (Util.isEmpty(avd)) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, MessageFormat.format("No AVD specified (modify your launch configuration).", avd)));
		}
		if (!android.hasAVD(avd)) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, MessageFormat.format("No AVD found with name {0} (modify your launch configuration).", avd)));
		}

		boolean mightNeedADBRestart = mightNeedADBRestart(adb, emulator);
		if (mightNeedADBRestart) {
			adb.killServer();
		}
		List<IAndroidEmulatorProcess> runningEmulators = emulator.getRunningProcesses(avd);
		IAndroidEmulatorProcess process = runningEmulators.size() > 0 ? runningEmulators.get(0) : null;
		if (process == null) {
			process = emulator.start(avd, true);
			startLogCat(adb);
		}
		
		DebugPlugin.newProcess(launch, process.getNativeProcess(), MessageFormat.format("Android Emulator ''{0}''", avd));

		// We need to wait until we're started.
		process.awaitEmulatorStarted(2, TimeUnit.MINUTES);

		IProject project = EmulatorLaunchConfigurationDelegate.getProject(launchConfig);

    	File packageToInstall = getPackageToInstall(launchConfig, mode);
    	if (packageToInstall != null) {
    		String serialNumberOfDevice = process.getEmulatorId();
    		adb.install(packageToInstall, MoSyncProject.create(project).getProperty(PropertyInitializer.ANDROID_PACKAGE_NAME), serialNumberOfDevice, new ProcessKiller(monitor));
    		if (!monitor.isCanceled()) {
    			adb.launch(Activator.getAndroidComponentName(MoSyncProject.create(project)), serialNumberOfDevice, new ProcessKiller(monitor));
    		}
        } else {
        	throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Project not built or build failed"));
        }
	}

	private boolean mightNeedADBRestart(ADB adb, Emulator emulator) throws CoreException {
		// Sometimes we loose the adb connection and then we will not see any emulators.
		// But since we keep track of emulator processes we can detect this condition
		// if at least one process is running. Which is ok, since that's usually when
		// the problem manifests itself
		List<String> adbEmulators = adb.listEmulators(false);
		List<IAndroidEmulatorProcess> runningProcesses = emulator.getAllRunningProcesses();
		// So... if no emulators according to adb but process according to ourselves -> restart!
		if (CoreMoSyncPlugin.getDefault().isDebugging()) {
			CoreMoSyncPlugin.trace("ADB processes: {0}\nEmulator tracked processes: {1}", adbEmulators, runningProcesses);
		}
		return (adbEmulators.isEmpty() && !runningProcesses.isEmpty());
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
		final boolean needsConfig = !shouldAskUserForLauncher(config, mode);

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
