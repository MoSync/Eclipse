package com.mobilesorcery.sdk.builder.android.launch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.builder.android.Activator;
import com.mobilesorcery.sdk.builder.android.launch.Emulator.IAndroidEmulatorProcess;
import com.mobilesorcery.sdk.core.AbstractTool;
import com.mobilesorcery.sdk.core.CollectingLineHandler;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;

public class Emulator extends AbstractTool {

	public static interface IAndroidEmulatorProcess {
		/**
		 * Returns the avd associated with this process.
		 */
		public String getAVD();

		/**
		 * Returns the emulator identifier associated with this process.
		 * @return
		 */
		public String getEmulatorId();

		/**
		 * Waits for this emulator to be booted and ready for uploading APKs
		 * @param timeout
		 * @param unit
		 * @throws CoreException If a timeout occurred, or if some other error occurred.
		 */
		public void awaitEmulatorStarted(int timeout, TimeUnit unit) throws CoreException;

	}

	private class AndroidEmulatorProcess extends CollectingLineHandler implements IAndroidEmulatorProcess {

		private final String avd;
		private String emulatorId;
		private final List<String> initialEmulators;
		private final Emulator emulator;
		private boolean started;

		AndroidEmulatorProcess(Emulator emulator, String avd) throws CoreException {
			this.avd = avd;
			this.emulator = emulator;
			initialEmulators = ADB.getExternal().listEmulators(false);
			emulator.associate(avd, this);
		}

		@Override
		public void start(Process process) {
			super.start(process);
		}

		@Override
		public void stop(IOException e) {
			emulator.disassociate(avd, this);
			super.stop(e);
		}

		@Override
		public String getAVD() {
			return avd;
		}

		@Override
		public String getEmulatorId() {
			return emulatorId;
		}

		@Override
		public synchronized void awaitEmulatorStarted(int timeout, TimeUnit unit) throws CoreException {
			if (started) {
				return;
			}
			awaitEmulatorStarted(ADB.getExternal(), this, timeout, unit);
			started = true;
		}

		private void awaitEmulatorStarted(ADB adb, CollectingLineHandler emulatorProcess, int timeout, TimeUnit unit) throws CoreException {
			long now = System.currentTimeMillis();
			long timeoutInMs = TimeUnit.MILLISECONDS.convert(timeout, unit);
			boolean wasStopped = emulatorProcess.isStopped();
			while (!wasStopped && System.currentTimeMillis() - now < timeoutInMs) {
				List<String> recentlyStarted = adb.listEmulators(false);
				recentlyStarted.removeAll(initialEmulators);
				if (recentlyStarted.size() > 0) {
					setEmulatorId(recentlyStarted.get(0));
					adb.awaitBoot(emulatorId, TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES));
					return;
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

		private void setEmulatorId(String emulatorId) {
			this.emulatorId = emulatorId;
		}

	}

	private final HashMap<String, List<IAndroidEmulatorProcess>> avdToProcess = new HashMap<String, List<IAndroidEmulatorProcess>>();

	public Emulator(IPath pathToEmulator) {
		super(pathToEmulator);
	}

	private static IPath externalSDKPath = null;
	private static Emulator externalEmulator = null;

	public synchronized AndroidEmulatorProcess start(String avd, boolean fork, String... options) throws CoreException {
		String[] commandLine = new String[options.length + 3];
		commandLine[0] = getToolPath().getAbsolutePath();
		commandLine[1] = "-avd";
		commandLine[2] = avd;
		System.arraycopy(options, 0, commandLine, 3, options.length);
		AndroidEmulatorProcess result = new AndroidEmulatorProcess(this, avd);
		execute(commandLine, result, null, fork);
		return result;
	}

	public static Emulator getExternal() {
		IPath sdkPath = Activator.getDefault().getExternalAndroidSDKPath();
		if (externalEmulator == null || !Util.equals(sdkPath, externalSDKPath)) {
			externalSDKPath = sdkPath;
			externalEmulator = new Emulator(sdkPath == null ? null : sdkPath.append("tools/emulator" + MoSyncTool.getBinExtension()));
		}
		return externalEmulator;
	}

	synchronized void associate(String avd, IAndroidEmulatorProcess emulatorProcess) {
		List<IAndroidEmulatorProcess> processes = avdToProcess.get(avd);
		if (processes == null) {
			processes = new ArrayList<IAndroidEmulatorProcess>();
			avdToProcess.put(avd, processes);
		}
		processes.add(emulatorProcess);
	}

	synchronized void disassociate(String avd, IAndroidEmulatorProcess emulatorProcess) {
		List<IAndroidEmulatorProcess> processes = avdToProcess.get(avd);
		if (processes != null) {
			processes.remove(emulatorProcess);
		}
		if (processes.isEmpty()) {
			avdToProcess.remove(avd);
		}
	}

	public synchronized List<IAndroidEmulatorProcess> getRunningProcesses(String avd) {
		List<IAndroidEmulatorProcess> runningProcesses = avdToProcess.get(avd);
		if (runningProcesses == null) {
			runningProcesses = new ArrayList<Emulator.IAndroidEmulatorProcess>();
		}

		return Collections.unmodifiableList(runningProcesses);
	}

	public synchronized List<IAndroidEmulatorProcess> getAllRunningProcesses() {
		List<IAndroidEmulatorProcess> allProcesses = new ArrayList<Emulator.IAndroidEmulatorProcess>();
		for (String avd : avdToProcess.keySet()) {
			List<IAndroidEmulatorProcess> processes = avdToProcess.get(avd);
			allProcesses.addAll(processes);
		}
		return allProcesses;
	}

	@Override
	protected String getToolName() {
		return "Android Emulator";
	}

}
