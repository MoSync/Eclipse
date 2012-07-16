package com.mobilesorcery.sdk.builder.iphoneos.launch;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.mobilesorcery.sdk.builder.iphoneos.Activator;
import com.mobilesorcery.sdk.builder.iphoneos.IPhoneOSPackager;
import com.mobilesorcery.sdk.builder.iphoneos.IPhoneSimulator;
import com.mobilesorcery.sdk.builder.iphoneos.SDK;
import com.mobilesorcery.sdk.builder.iphoneos.XCodeBuild;
import com.mobilesorcery.sdk.builder.iphoneos.ui.dialogs.ConfigureXcodeDialog;
import com.mobilesorcery.sdk.core.BuildVariant;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IPackager;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Version;
import com.mobilesorcery.sdk.core.launch.AbstractEmulatorLauncher;
import com.mobilesorcery.sdk.core.launch.IEmulatorLauncher;
import com.mobilesorcery.sdk.internal.launch.EmulatorLaunchConfigurationDelegate;

public class IPhoneEmulatorLauncher extends AbstractEmulatorLauncher {

	public final static String SDK_ATTR = "iphone.sdk";

	private static final String ID = "com.mobilesorcery.sdk.builder.iphoneos.launcher";

	public IPhoneEmulatorLauncher() {
		super("iPhone Simulator");
	}

	@Override
	public int isLaunchable(ILaunchConfiguration launchConfiguration, String mode) {
		if (!Util.isMac()) {
			return UNLAUNCHABLE;
		} else if (!isCorrectPackager(launchConfiguration)) {
			return IEmulatorLauncher.UNLAUNCHABLE;
		} if (shouldAskUserForLauncher(launchConfiguration, mode)) {
			return IEmulatorLauncher.REQUIRES_CONFIGURATION;
		} else if (!isCorrectlyInstalled()) {
			IEmulatorLauncher preferredLauncher = CoreMoSyncPlugin.getDefault().getPreferredLauncher(IPhoneOSPackager.ID);
			boolean useOtherLauncher = !shouldAskUserForLauncher(IPhoneOSPackager.ID) && !Util.equals(preferredLauncher.getId(), ID);
			return isAutoSelectLaunch(launchConfiguration, mode) && useOtherLauncher ?
					IEmulatorLauncher.UNLAUNCHABLE :
					IEmulatorLauncher.REQUIRES_CONFIGURATION;
		} else {
			return super.isLaunchable(launchConfiguration, mode);
		}
	}

	private boolean shouldAskUserForLauncher(ILaunchConfiguration launchConfiguration, String mode) {
		return isCorrectlyInstalled() && isAutoSelectLaunch(launchConfiguration, mode) && shouldAskUserForLauncher(IPhoneOSPackager.ID);
	}

	@Override
	public void launch(ILaunchConfiguration launchConfig, String mode,
			ILaunch launch, int emulatorId, IProgressMonitor monitor)
			throws CoreException {
		IPhoneSimulator sim = IPhoneSimulator.createDefault();
		sim.assertValid();
		// TODO: Incremental building if we change the SDK!?
		IProject project = EmulatorLaunchConfigurationDelegate.getProject(launchConfig);
		MoSyncProject mosyncProject = MoSyncProject.create(project);
		SDK sdk = Activator.getDefault().getSDK(mosyncProject, XCodeBuild.IOS_SIMULATOR_SDKS);
		Version sdkVersion = sdk == null ? null : sdk.getVersion();
		File pathToApp = getPackageToInstall(launchConfig, mode);
		String family = getFamily(getVariant(launchConfig, mode));
		Process process = sim.runApp(new Path(pathToApp.getAbsolutePath()), sdkVersion == null ? null : sdkVersion.toString(), family);
		
		DebugPlugin.newProcess(launch, process, MessageFormat.format("iPhone Simulator {0}", sdk.getVersion()));
	}

	private String getFamily(IBuildVariant variant) {
		// Hard-coded, we may want to get this from device db instead.
		if (variant.getProfile().getName().contains("iPad")) {
			return "ipad";
		}
		return null;
	}

	@Override
	public IBuildVariant getVariant(ILaunchConfiguration launchConfig, String mode) throws CoreException {
		IBuildVariant prototype = super.getVariant(launchConfig, mode);
		BuildVariant modified = new BuildVariant(prototype);
		modified.setSpecifier(Activator.IOS_SIMULATOR_SPECIFIER, Activator.IOS_SIMULATOR_SPECIFIER);
		return modified;
	}

	@Override
	public IEmulatorLauncher configure(ILaunchConfiguration config, String mode) {
		XCodeBuild.getDefault().refresh();

		Display d = PlatformUI.getWorkbench().getDisplay();
		// If we are not auto-select, don't fallback to MoRe.
		final boolean isAutomaticSelection = isAutoSelectLaunch(config, mode);
		// And if we are supposed to ask the user, we do not really need to configure anything.
		final boolean needsConfig = !shouldAskUserForLauncher(config, mode);

		final IEmulatorLauncher[] result = new IEmulatorLauncher[] { null };
		d.syncExec(new Runnable() {
			@Override
			public void run() {
				// OK, figure out after 2.6 release where to really put this ui stuff!
				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				ConfigureXcodeDialog configureDialog = new ConfigureXcodeDialog(shell);
				configureDialog.setIsAutomaticSelection(isAutomaticSelection);
				configureDialog.setNeedsConfig(needsConfig);
				configureDialog.open();
				result[0] = configureDialog.getSelectedLauncher();
			}
		});
		return result[0];
	}

	@Override
	public int getLaunchType(IPackager packager) {
		return Util.equals(packager.getId(), IPhoneOSPackager.ID) && Util.isMac() ? LAUNCH_TYPE_NATIVE : LAUNCH_TYPE_NONE;
	}

	protected boolean isCorrectlyInstalled() {
		return XCodeBuild.getDefault().isValid() && XCodeBuild.getDefault().listSDKs(XCodeBuild.IOS_SIMULATOR_SDKS).size() > 0;
	}
}
