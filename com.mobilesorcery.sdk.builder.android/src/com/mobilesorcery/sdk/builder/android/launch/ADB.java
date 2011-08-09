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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.builder.android.Activator;
import com.mobilesorcery.sdk.core.AbstractTool;
import com.mobilesorcery.sdk.core.CollectingLineHandler;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;

/**
 * A class representing the Android Debug Bridge.
 * 
 * @author Mattias Bybro
 * 
 */
public class ADB extends AbstractTool {

	private static ADB instance = new ADB();
	
	private ADB() {
		this(MoSyncTool.getDefault().getBinary("android/adb"));
	}

	public ADB(IPath pathToADB) {
		super(pathToADB);
	}
	
	public static ADB getDefault() {
		return instance;
	}
	
	public static ADB getExternal() {
		IPath sdkPath = Activator.getDefault().getExternalAndroidSDKPath();
		return new ADB(sdkPath == null ? null : sdkPath.append("platform-tools/adb" + MoSyncTool.getBinExtension()));
	}
	
	/**
	 * Returns a list of all online android devices (no emulators)
	 * @return
	 * @throws CoreException
	 */

	public List<String> listDeviceSerialNumbers(boolean useConsole) throws CoreException {
		return listDevices(false, true, useConsole);
	}
	
	/**
	 * Returns a list of all online android emulators (no real devices)
	 * @return
	 * @throws CoreException
	 */
	public List<String> listEmulators(boolean useConsole) throws CoreException {
		return listDevices(true, false, useConsole);
	}
	
	private List<String> listDevices(boolean emulators, boolean realDevices, boolean useConsole) throws CoreException {
		CollectingLineHandler collectingLineHandler = new CollectingLineHandler();
		execute(
				new String[] { getToolPath().getAbsolutePath(), "devices" },
				collectingLineHandler, collectingLineHandler, false);
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
							boolean isEmulator = serialNumber.startsWith("emulator-");
							if ((realDevices && !isEmulator) || (emulators && isEmulator)) {
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

	public void install(File packageToInstall, String serialNumberOfDevice) throws CoreException {
		CollectingLineHandler collectingLineHandler = new CollectingLineHandler();
		CollectingLineHandler errorLineHandler = new CollectingLineHandler();
		int errorCode = execute(new String[] { getToolPath().getAbsolutePath(),
				"-s",
				serialNumberOfDevice,
				"install",
				"-r",
				packageToInstall.getAbsolutePath()
		}, collectingLineHandler, errorLineHandler, false);
		
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
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					MessageFormat.format("Could not install on device: {0}", errorMsg)));
		}
	}
	
	public void launch(String activityName, String serialNumberOfDevice) throws CoreException {
		CollectingLineHandler collectingLineHandler = new CollectingLineHandler();
		CollectingLineHandler errorLineHandler = new CollectingLineHandler();
		int errorCode = execute(new String[] { getToolPath().getAbsolutePath(),
				"-s",
				serialNumberOfDevice,
				"shell",
				"am",
				"start",
				"-n",
				activityName
		}, collectingLineHandler, errorLineHandler, false);
		
	}

	@Override
	protected String getToolName() {
		return "ADB";
	}
}
