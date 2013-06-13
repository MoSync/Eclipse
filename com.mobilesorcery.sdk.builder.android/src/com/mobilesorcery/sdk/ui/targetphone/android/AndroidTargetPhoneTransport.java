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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.cdt.debug.mi.core.IMILaunchConfigurationConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.dialogs.ListDialog;

import com.mobilesorcery.sdk.builder.android.Activator;
import com.mobilesorcery.sdk.builder.android.AndroidPackager;
import com.mobilesorcery.sdk.builder.android.PropertyInitializer;
import com.mobilesorcery.sdk.builder.android.launch.ADB;
import com.mobilesorcery.sdk.builder.android.launch.AndroidNDKDebugger;
import com.mobilesorcery.sdk.builder.android.launch.ADB.ProcessKiller;
import com.mobilesorcery.sdk.builder.android.launch.AndroidEmulatorLauncher;
import com.mobilesorcery.sdk.core.AbstractTool;
import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.ILaunchConstants;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.internal.launch.EmulatorLaunchConfigurationDelegate;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.ProfileDBManager;
import com.mobilesorcery.sdk.profiles.filter.AbstractDeviceFilter;
import com.mobilesorcery.sdk.ui.internal.launch.MoreLaunchShortCut;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhone;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhoneTransportDelegate;
import com.mobilesorcery.sdk.ui.targetphone.TargetPhonePlugin;
import com.mobilesorcery.sdk.ui.targetphone.TargetPhoneTransportEvent;

public class AndroidTargetPhoneTransport implements ITargetPhoneTransportDelegate {

	public static final String ID = "android";

	static final Pattern androidPlatformRegexp = Pattern.compile("^profiles\\\\runtimes\\\\android.*");
	private static final IDeviceFilter ANDROID_DEVICE_FILTER = new AbstractDeviceFilter() {
		@Override
		public boolean acceptProfile(IProfile profile) {
			return "android".equalsIgnoreCase(ProfileDBManager.getPlatform(profile));
		}

		@Override
		public String getFactoryId() {
			return null;
		}

		@Override
		public void saveState(IMemento memento) {
		}
	};

	public AndroidTargetPhoneTransport() {
	}

	@Override
	public ITargetPhone load(IMemento memento, String name) {
		String serialNo = memento.getString("serialno");
		return new AndroidTargetPhone(name, serialNo, ID);
	}

	@Override
	public boolean store(ITargetPhone phone, IMemento memento) {
		if (phone instanceof AndroidTargetPhone) {
			AndroidTargetPhone androidPhone = (AndroidTargetPhone) phone;
			memento.putString("serialno", androidPhone.getSerialNumber());
			return true;
		} else {
			return false;
		}
	}

	@Override
	public ITargetPhone scan(IShellProvider shellProvider, IProgressMonitor monitor) throws CoreException {
		final List<String> devices = ADB.getDefault().listDeviceSerialNumbers(true);
		if (devices.size() == 0) {
			throw new CoreException(new Status(IStatus.ERROR, TargetPhonePlugin.PLUGIN_ID, "No android devices connected"));
		}

		final AndroidTargetPhone[] phone = new AndroidTargetPhone[1];
		final Shell shell = shellProvider.getShell();

		shell.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				ListDialog dialog = new ListDialog(shell);
				dialog.setTitle("Android Device");
				dialog.setMessage("Select Android device to use");
				dialog.setContentProvider(new ArrayContentProvider());
				dialog.setLabelProvider(new LabelProvider());
				dialog.setInput(devices.toArray());
				if (dialog.open() == ListDialog.OK) {
					Object[] dialogResult = dialog.getResult();
					if (dialogResult.length > 0) {
						String serialNumber = (String) dialogResult[0];
						String modelName = ADB.getDefault().getModelName(serialNumber);
						String name = Util.isEmpty(modelName) ? serialNumber : modelName;
						phone[0] = new AndroidTargetPhone(name, serialNumber, AndroidTargetPhoneTransport.ID);
					}
				}
			}
		});

		return phone[0];
	}

	@Override
	public void send(IShellProvider shell, final MoSyncProject project, final IBuildVariant variant, ITargetPhone phone, File packageToSend, IProgressMonitor monitor)
			throws CoreException {
		if (phone instanceof AndroidTargetPhone) {
			AndroidTargetPhone androidPhone = (AndroidTargetPhone) phone;
			final String serialNumberOfDevice = androidPhone.getSerialNumber();
			final String mode = project.getBuildConfiguration(variant.getConfigurationId()).getTypes().contains(IBuildConfiguration.DEBUG_TYPE) ?
					"debug" : "run";
			final ILaunchConfiguration[] cfg = new ILaunchConfiguration[1];
			final CoreException[] exception = new CoreException[1];
			shell.getShell().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					try {
						cfg[0] = AndroidEmulatorLauncher.findLaunchConfiguration(project, variant, mode, true, serialNumberOfDevice);
					} catch (CoreException e) {
						exception[0] = e;
					}
				}
				
			});
			if (exception[0] != null) {
				throw exception[0];
			}
			if (cfg[0] != null) {
				cfg[0].launch(mode, monitor, false);	
			}
		} else {
			throw new CoreException(new Status(IStatus.ERROR, TargetPhonePlugin.PLUGIN_ID, "Can only send to android phones"));
		}
	}

	@Override
	public String getDescription(String context) {
		return "Android USB";
	}

	@Override
	public boolean isAvailable() {
		return ADB.getDefault().isValid();
	}

	@Override
	public IDeviceFilter getAcceptedProfiles() {
		return ANDROID_DEVICE_FILTER;
	}

}
