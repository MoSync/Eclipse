/*  Copyright (C) 2010 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.mobilesorcery.sdk.builder.android.launch;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import com.mobilesorcery.sdk.builder.android.Activator;
import com.mobilesorcery.sdk.builder.android.PropertyInitializer;
import com.mobilesorcery.sdk.core.AbstractTool;
import com.mobilesorcery.sdk.core.CollectingLineHandler;
import com.mobilesorcery.sdk.core.CommandLineExecutor;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.LineReader;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;

/**
 * A class representing the Android Debug Bridge.
 *
 * @author Mattias Bybro
 *
 */
public class ADB extends AbstractTool {

	private final class ProcessKiller extends LineReader.LineAdapter {
		private Process process;

		@Override
		public void stop(IOException e) {
			process = null;
		}

		@Override
		public void start(Process process) {
			this.process = process;
		}

		public void killProcess() {
			if (process != null) {
				process.destroy();
			}
		}
	}

	private final class LogcatListener implements IPropertyChangeListener {
		private ADB adb;

		public LogcatListener(ADB adb) {
			this.adb = adb;
		}

		@Override
		public void propertyChange(PropertyChangeEvent event) {
			if (PropertyInitializer.ADB_DEBUG_LOG.equals(event.getProperty()) ||
				PropertyInitializer.ADB_LOGCAT_ARGS.equals(event.getProperty())) {
				try {
					adb.startLogCat();
				} catch (CoreException e) {
					CoreMoSyncPlugin.getDefault().log(e);
				}
			}
		}
	}

	private static ADB instance = new ADB();
	private boolean logcatStarted;
	private IPropertyChangeListener logCatListener = null;
	private ProcessKiller logcatProcessHandler;

	private ADB() {
		this(MoSyncTool.getDefault().getBinary("android/adb"));
	}

	public ADB(IPath pathToADB) {
		super(pathToADB);
		logCatListener = new LogcatListener(this);
		logcatProcessHandler = new ProcessKiller();
	}

	public static ADB getDefault() {
		return instance;
	}

	public static ADB getExternal() {
		IPath sdkPath = Activator.getDefault().getExternalAndroidSDKPath();
		return findADB(sdkPath);
	}

	/**
	 * Tries to locate the proper ADB to use; this location may differ depending
	 * on which Android SDK is installed.
	 * @param sdkRootPath The Android SDK root directory
	 * @return Does not return {@code null} unless {@code sdkRootPath} is {@code null}.
	 */
	public static ADB findADB(IPath sdkRootPath) {
		if (sdkRootPath == null) {
			return null;
		}

		IPath primaryADBPath = sdkRootPath.append("platform-tools/adb" + MoSyncTool.getBinExtension());
		ADB primaryADB = new ADB(primaryADBPath);
		if (!primaryADB.isValid()) {
			// Older Android SDK has the adb tool elsewhere
			IPath secondaryADBPath = sdkRootPath.append("tools/adb" + MoSyncTool.getBinExtension());
			ADB secondaryADB = new ADB(secondaryADBPath);
			if (secondaryADB.isValid()) {
				return secondaryADB;
			}
		}

		return primaryADB;
	}

	/**
	 * Returns a list of all online android devices (no emulators)
	 *
	 * @return
	 * @throws CoreException
	 */

	public List<String> listDeviceSerialNumbers(boolean useConsole)
			throws CoreException {
		return listDevices(false, true, useConsole);
	}

	/**
	 * Returns a list of all online android emulators (no real devices)
	 *
	 * @return
	 * @throws CoreException
	 */
	public List<String> listEmulators(boolean useConsole) throws CoreException {
		return listDevices(true, false, useConsole);
	}

	private List<String> listDevices(boolean emulators, boolean realDevices,
			boolean useConsole) throws CoreException {
		CollectingLineHandler collectingLineHandler = new CollectingLineHandler();
		execute(new String[] { getToolPath().getAbsolutePath(), "devices" },
				collectingLineHandler, collectingLineHandler, CoreMoSyncPlugin.LOG_CONSOLE_NAME, false);
		ArrayList<String> result = new ArrayList<String>();
		for (String line : collectingLineHandler.getLines()) {
			line = line.trim();
			if (line.length() > 0) {
				if (!line.startsWith("*") && !line.startsWith("List")) {
					// Then heuristically, we have a device!
					String[] device = line.split("\\s+");
					if (device.length > 1) {
						String serialNumber = device[0];
						String state = device[device.length - 1];
						if ("device".equals(state)) {
							boolean isEmulator = serialNumber
									.startsWith("emulator-");
							if ((realDevices && !isEmulator)
									|| (emulators && isEmulator)) {
								// Only include online devices and no emulators
								result.add(serialNumber);
							}
						}
					}
				}
			}
		}

		return result;
	}

	public void uninstall(String packageName, String serialNumberOfDevice) throws CoreException {
		runAndCollectError(new String[] { getToolPath().getAbsolutePath(), "-s",
				serialNumberOfDevice, "uninstall", packageName });
	}
	
	public void install(File packageToInstall, String packageName, String serialNumberOfDevice) throws CoreException {
		if (packageName != null && Activator.getDefault().getPreferenceStore().getBoolean(PropertyInitializer.ADB_UNINSTALL_FIRST)) {
			try {
				uninstall(packageName, serialNumberOfDevice);
			} catch (CoreException e) {
				// Ignore -- the apk might not exist.
			}
		}
		runAndCollectError(new String[] { getToolPath().getAbsolutePath(), "-s",
						serialNumberOfDevice, "install", "-r",
						packageToInstall.getAbsolutePath() });
	}
	
	private void runAndCollectError(String[] commandLine)
			throws CoreException {
		CollectingLineHandler collectingLineHandler = new CollectingLineHandler();
		CollectingLineHandler errorLineHandler = new CollectingLineHandler();
		int errorCode = execute(commandLine,
				collectingLineHandler, errorLineHandler, false);

		String errorMsg = null;
		for (String line : collectingLineHandler.getLines()) {
			if (line.trim().startsWith("Failure")) {
				errorMsg = line;
				errorCode = -127;
			}
		}

		List<String> errorLines = errorLineHandler.getLines();
		if (errorLines.size() > 0) {
			String error = Util.join(errorLines.toArray(), "\n").trim();
			if (!Util.isEmpty(error) && errorMsg == null) {
				errorMsg = error;
			}
		}

		if (errorCode != 0 && errorMsg != null) {
			throw new CoreException(new Status(IStatus.ERROR,
					Activator.PLUGIN_ID, MessageFormat.format(
							"Could not install on device: {0}", errorMsg)));
		}
	}

	public void launch(String activityName, String serialNumberOfDevice)
			throws CoreException {
		CollectingLineHandler collectingLineHandler = new CollectingLineHandler();
		CollectingLineHandler errorLineHandler = new CollectingLineHandler();
		int errorCode = execute(new String[] { getToolPath().getAbsolutePath(),
				"-s", serialNumberOfDevice, "shell", "am", "start", "-n",
				activityName }, collectingLineHandler, errorLineHandler, false);

	}

	@Override
	protected String getToolName() {
		return "ADB";
	}

	public void awaitBoot(String serialNumberOfDevice, long timeoutInMs) throws CoreException {
		long startTime = System.currentTimeMillis();
		while (!isBootComplete(serialNumberOfDevice)) {
			if (System.currentTimeMillis() - startTime > timeoutInMs) {
				throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Connection to Android Emulator timed out"));
			}
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Connection to Android Emulator timed out", e));
			}
		}
	}

	public boolean isBootComplete(String serialNumberOfDevice) throws CoreException {
		String reply = getProp(serialNumberOfDevice, "dev.bootcomplete");
		return "1".equals(reply);
	}

	public String getProp(String serialNumberOfDevice, String prop) throws CoreException {
		CollectingLineHandler cl = new CollectingLineHandler();
		execute(new String[] { getToolPath().getAbsolutePath(), "-s",
				serialNumberOfDevice, "shell", "getprop", prop
		}, cl, cl, CoreMoSyncPlugin.LOG_CONSOLE_NAME, false);
		String reply = cl.getFirstLine().trim();
		return reply;
	}

	public void setProp(String serialNumberOfDevice, String propKey, String propValue) throws CoreException {
		CollectingLineHandler cl = new CollectingLineHandler();
		execute(new String[] { getToolPath().getAbsolutePath(), "-s",
				serialNumberOfDevice, "shell", "setprop", propKey, propValue
		}, cl, cl, CoreMoSyncPlugin.LOG_CONSOLE_NAME, false);
	}

	public synchronized void startLogCat() throws CoreException {
		IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
		if (!logcatStarted) {
			logcatStarted = true;
			prefs.addPropertyChangeListener(logCatListener);
		}
		boolean silent = !prefs.getBoolean(PropertyInitializer.ADB_DEBUG_LOG);

		logcatProcessHandler.killProcess();
		if (!silent) {
				// Then restart!
			ArrayList<String> commandLine = new ArrayList<String>();
			commandLine.add(getToolPath().getAbsolutePath());
			commandLine.add("logcat");
			String[] args = CommandLineExecutor.parseCommandLine(prefs.getString(PropertyInitializer.ADB_LOGCAT_ARGS));
			commandLine.addAll(Arrays.asList(args));
			
			// We never have more than one logcat process.
			execute(commandLine.toArray(new String[0]), logcatProcessHandler, null, true);
		}
	}
	
	private synchronized void stopLogCat() {
		logcatProcessHandler.killProcess();
		logcatStarted = false;
		IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
		prefs.removePropertyChangeListener(logCatListener);
	}

	public synchronized void killServer() throws CoreException {
		execute(new String[] { getToolPath().getAbsolutePath(),
				"kill-server"
		}, null, null, CoreMoSyncPlugin.LOG_CONSOLE_NAME, false);
		stopLogCat();
	}
}
