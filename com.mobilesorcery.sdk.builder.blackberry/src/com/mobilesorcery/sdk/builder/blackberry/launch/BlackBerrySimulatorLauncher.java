package com.mobilesorcery.sdk.builder.blackberry.launch;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import com.mobilesorcery.sdk.builder.blackberry.BlackBerryPackager;
import com.mobilesorcery.sdk.builder.blackberry.BlackBerryPlugin;
import com.mobilesorcery.sdk.core.CommandLineExecutor;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IProcessConsole;
import com.mobilesorcery.sdk.core.LineReader;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.launch.AbstractEmulatorLauncher;

// BIG PHAT TODO ON THIS CLASS; MAY BE USED AS A STARTING POINT
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
	public void launch(ILaunchConfiguration launchConfig, String mode,
			ILaunch launch, int emulatorId, IProgressMonitor monitor)
			throws CoreException {
		IPath fc = getSDKPath(launchConfig).append("fledgecontroller.exe");

		final File packageToInstall = getPackageToInstall(launchConfig, mode);
		CommandLineExecutor executor = new CommandLineExecutor(MoSyncBuilder.CONSOLE_ID);
		executor.setLineHandlers(new SimulatorProcessHandler(packageToInstall), null);
		
		executor.addCommandLine(new String[] {
				fc.toFile().getAbsolutePath()
		});
		
		try {
			executor.execute();
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, BlackBerryPlugin.PLUGIN_ID, e.getMessage(), e));
		}
	}

	private IPath getSDKPath(ILaunchConfiguration launchConfig) throws CoreException {
		return new Path(launchConfig.getAttribute(BlackBerryPlugin.SDK_PATH, ""));
	}
	
	@Override
	public boolean isAvailable(ILaunchConfiguration config, String mode) {
		return super.isAvailable(config, mode);
	}

	@Override
	public void assertLaunchable(ILaunchConfiguration config, String mode) throws CoreException {
		assertWindows();
		assertCorrectPackager(config, BlackBerryPackager.ID, "The BlackBerry Simulator requires the target profile to be a BlackBerry device");
		super.assertLaunchable(config, mode);
	}
}
