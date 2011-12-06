package com.mobilesorcery.sdk.builder.blackberry.launch;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.mobilesorcery.sdk.builder.blackberry.BlackBerryPackager;
import com.mobilesorcery.sdk.builder.blackberry.BlackBerryPlugin;
import com.mobilesorcery.sdk.builder.blackberry.JDE;
import com.mobilesorcery.sdk.builder.blackberry.Simulator;
import com.mobilesorcery.sdk.core.CommandLineExecutor;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IPackager;
import com.mobilesorcery.sdk.core.IProcessConsole;
import com.mobilesorcery.sdk.core.LineReader;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.launch.AbstractEmulatorLauncher;
import com.mobilesorcery.sdk.core.launch.IEmulatorLauncher;
import com.mobilesorcery.sdk.internal.launch.EmulatorLaunchConfigurationDelegate;

public class BlackBerrySimulatorLauncher extends AbstractEmulatorLauncher {

	private final class SimulatorProcessHandler extends LineReader.LineAdapter {
		private final File packageToInstall;

		private SimulatorProcessHandler(File packageToInstall) {
			this.packageToInstall = packageToInstall;
		}

		@Override
		public void start(Process process) {
			try {
				IProcessConsole console = CoreMoSyncPlugin.getDefault().createConsole(MoSyncBuilder.CONSOLE_ID);
				OutputStream output = process.getOutputStream();
				write(console, output, String.format("loadcod(%s)\n", packageToInstall.getAbsolutePath()));
			} catch (Exception e) {
				CoreMoSyncPlugin.getDefault().log(e);
			}
		}

		private void write(IProcessConsole console, OutputStream output,
				String msg) throws IOException {
			console.addMessage(msg);
			output.write(msg.getBytes());
		}
	}

	public BlackBerrySimulatorLauncher() {
		super("BlackBerry Simulator");
	}

	@Override
	public int isLaunchable(ILaunchConfiguration launchConfiguration, String mode) {
		/*if (!Util.isWindows()) {
			return UNLAUNCHABLE;
		} else*/ if (!isCorrectPackager(launchConfiguration)) {
			return UNLAUNCHABLE;
		} else if (shouldAskUserForLauncher(launchConfiguration, mode)) {
			return REQUIRES_CONFIGURATION;
		} else {
			return super.isLaunchable(launchConfiguration, mode);
		}
	}

	private boolean shouldAskUserForLauncher(ILaunchConfiguration launchConfiguration, String mode) {
		return isCorrectlyInstalled(launchConfiguration, mode) && isAutoSelectLaunch(launchConfiguration, mode) && shouldAskUserForLauncher(BlackBerryPackager.ID);
	}

	protected boolean isCorrectlyInstalled(ILaunchConfiguration launchConfiguration, String mode) {
		return getMatchingSimulator(launchConfiguration, mode) != null;
	}

	private Simulator getMatchingSimulator(ILaunchConfiguration launchConfiguration, String mode) {
		try {
			IBuildVariant variant = super.getVariant(launchConfiguration, mode);
			IProject project = EmulatorLaunchConfigurationDelegate.getProject(launchConfiguration);
			Simulator simulator = (Simulator) BlackBerryPackager.matchingJDE(
					JDE.TYPE_SIMULATOR, MoSyncProject.create(project),
					variant.getProfile());
			return simulator;
		} catch (CoreException e) {
			CoreMoSyncPlugin.getDefault().log(e);
			return null;
		}
	}

	@Override
	public IEmulatorLauncher configure(ILaunchConfiguration config, String mode) {
		Display d = PlatformUI.getWorkbench().getDisplay();
		// If we are not auto-select, don't fallback to MoRe.
		final boolean isAutomaticLaunch = isAutoSelectLaunch(config, mode);
		// And if we are supposed to ask the user, we do not really need to configure anything.
		final boolean needsConfig = !shouldAskUserForLauncher(config, mode);

		final IEmulatorLauncher[] result = new IEmulatorLauncher[] { null };
		d.syncExec(new Runnable() {
			@Override
			public void run() {
				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				result[0] = showConfigureDialog(shell, isAutomaticLaunch, needsConfig);
			}
		});
		return result[0];
	}


	protected IEmulatorLauncher showConfigureDialog(Shell shell,
			boolean showFallbackAlternative, boolean needsConfig) {
		ConfigureBlackBerryJDEDialog dialog = new ConfigureBlackBerryJDEDialog(shell);
		dialog.setIsAutomaticSelection(showFallbackAlternative);
		dialog.setNeedsConfig(needsConfig);
		dialog.open();
		return dialog.getSelectedLauncher();
	}

	@Override
	public void launch(ILaunchConfiguration launchConfig, String mode,
			ILaunch launch, int emulatorId, IProgressMonitor monitor)
			throws CoreException {
		IPath fledge = getSDKPath(launchConfig, mode).append("fledge.exe");
		IPath fc = getSDKPath(launchConfig, mode).append("fledgecontroller.exe");

		final File packageToInstall = getPackageToInstall(launchConfig, mode);
		CommandLineExecutor executor = new CommandLineExecutor(MoSyncBuilder.CONSOLE_ID);
		executor.setLineHandlers(new SimulatorProcessHandler(packageToInstall), null);

		executor.addCommandLine(new String[] {
			fledge.toFile().getAbsolutePath()
		});
		executor.addCommandLine(new String[] {
			fc.toFile().getAbsolutePath()
		});

		try {
			executor.execute();
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, BlackBerryPlugin.PLUGIN_ID, e.getMessage(), e));
		}
	}

	private IPath getSDKPath(ILaunchConfiguration launchConfig, String mode) throws CoreException {
		//return new Path(launchConfig.getAttribute(BlackBerryPlugin.SDK_PATH, ""));
		Simulator simulator = getMatchingSimulator(launchConfig, mode);
		if (simulator == null) {
			throw new CoreException(new Status(IStatus.ERROR, BlackBerryPlugin.PLUGIN_ID,
					"Found no simulator for this BlackBerry version."));
		}
		return simulator.getLocation();
	}

	@Override
	public int getLaunchType(IPackager packager) {
		return Util.equals(packager.getId(), BlackBerryPackager.ID) ? LAUNCH_TYPE_NATIVE : LAUNCH_TYPE_NONE;
	}

}
