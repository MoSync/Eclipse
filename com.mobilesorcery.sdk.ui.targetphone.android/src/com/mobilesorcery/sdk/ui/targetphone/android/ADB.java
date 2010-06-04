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
package com.mobilesorcery.sdk.ui.targetphone.android;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.CommandLineExecutor;
import com.mobilesorcery.sdk.core.LineReader;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.LineReader.ILineHandler;
import com.mobilesorcery.sdk.ui.targetphone.TargetPhonePlugin;

/**
 * A class representing the Android Debug Bridge.
 * 
 * @author Mattias Bybro
 * 
 */
public class ADB {

	public class CollectingLineHandler implements ILineHandler {

		ArrayList<String> lines = new ArrayList<String>();

		public void newLine(String line) {
			lines.add(line);
			System.out.println(line);
		}

		public void stop(IOException e) {
		}

		public List<String> getLines() {
			return lines;
		}

	}

	private static ADB instance = new ADB();
	private IPath pathToADB;
	private boolean valid;

	private ADB() {
		this(MoSyncTool.getDefault().getBinary("android/adb"));
	}

	public ADB(IPath pathToADB) {
		this.pathToADB = pathToADB;
		valid = pathToADB.toFile().exists();
	}

	public boolean isValid() {
		return valid;
	}
	
	public static ADB getDefault() {
		return instance;
	}
	
	/**
	 * Returns a list of all online android devices (no emulators)
	 * @return
	 * @throws CoreException
	 */

	public List<String> listDeviceSerialNumbers() throws CoreException {
		CollectingLineHandler collectingLineHandler = new CollectingLineHandler();
		execute(
				new String[] { pathToADB.toFile().getAbsolutePath(), "devices" },
				collectingLineHandler, collectingLineHandler);
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
						if ("device".equals(state) && !serialNumber.startsWith("emulator-")) {
							// Only include online devices and no emulators
							result.add(serialNumber);
						}
					}
				}
			}
		}

		return result;
	}

	public void install(File packageToInstall, String serialNumberOfDevice) throws CoreException {
		CollectingLineHandler collectingLineHandler = new CollectingLineHandler();
		CollectingLineHandler errorLineHandler = new CollectingLineHandler();
		int errorCode = execute(new String[] { pathToADB.toFile().getAbsolutePath(),
				"-s",
				serialNumberOfDevice,
				"install",
				"-r",
				packageToInstall.getAbsolutePath()
		}, collectingLineHandler, errorLineHandler);
		
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
			throw new CoreException(new Status(IStatus.ERROR, TargetPhonePlugin.PLUGIN_ID,
					MessageFormat.format("Could not install on device: {0}", errorMsg)));
		}
	}
	
	public void launch(String activityName, String serialNumberOfDevice) throws CoreException {
		CollectingLineHandler collectingLineHandler = new CollectingLineHandler();
		CollectingLineHandler errorLineHandler = new CollectingLineHandler();
		int errorCode = execute(new String[] { pathToADB.toFile().getAbsolutePath(),
				"-s",
				serialNumberOfDevice,
				"shell",
				"am",
				"start",
				"-n",
				activityName
		}, collectingLineHandler, errorLineHandler);
		
	}

	private int execute(String[] commandLine, ILineHandler stdoutLineHandler, ILineHandler stderrLineHandler)
			throws CoreException {
		try {
			CommandLineExecutor executor = new CommandLineExecutor(MoSyncBuilder.CONSOLE_ID);
			executor.setLineHandlers(stdoutLineHandler, stderrLineHandler);
			executor.addCommandLine(commandLine);
			return executor.execute();
			/*Process process = createProcess(commandLine);
			final InputStream input = process.getInputStream();
			final InputStream error = process.getErrorStream();

			Reader inputReader = new InputStreamReader(input);
			Reader errorReader = new InputStreamReader(error);

			LineReader inputPump = new LineReader(inputReader,
					stdoutLineHandler);
			LineReader errorPump = new LineReader(errorReader, stderrLineHandler);

			inputPump.start();
			errorPump.start();

			return process.waitFor();*/
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR,
					"com.mobilesorcery.sdk.ui.targetphone.android", e
							.getMessage(), e));
		}
	}

	private Process createProcess(String[] commandLine) throws IOException {
		return Runtime.getRuntime().exec(commandLine);
	}
}
