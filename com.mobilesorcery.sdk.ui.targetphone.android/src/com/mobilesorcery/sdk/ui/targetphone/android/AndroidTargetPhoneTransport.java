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
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.dialogs.ListDialog;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhone;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhoneTransportDelegate;
import com.mobilesorcery.sdk.ui.targetphone.TargetPhonePlugin;

public class AndroidTargetPhoneTransport implements ITargetPhoneTransportDelegate {

	private static final String ID = "android";

	public AndroidTargetPhoneTransport() {
	}

	public ITargetPhone load(IMemento memento, String name) {
		String serialNo = memento.getString("serialno");
		return new AndroidTargetPhone(name, serialNo, ID);
	}

	public boolean store(ITargetPhone phone, IMemento memento) {
		if (phone instanceof AndroidTargetPhone) {
			AndroidTargetPhone androidPhone = (AndroidTargetPhone) phone;
			memento.putString("serialno", androidPhone.getSerialNumber());
			return true;	
		} else {
			return false;
		}
	}

	public ITargetPhone scan(IShellProvider shellProvider, IProgressMonitor monitor) throws CoreException {
		final List<String> devices = ADB.getDefault().listDeviceSerialNumbers();
		if (devices.size() == 0) {
			throw new CoreException(new Status(IStatus.ERROR, TargetPhonePlugin.PLUGIN_ID, "No android devices connected"));
		}
		
		final AndroidTargetPhone[] phone = new AndroidTargetPhone[1];
		final Shell shell = shellProvider.getShell();
	
		shell.getDisplay().syncExec(new Runnable() {
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
						phone[0] = new AndroidTargetPhone(serialNumber, serialNumber, AndroidTargetPhoneTransport.ID);
					}
				}
			}
		});
		
		return phone[0];
	}

	public void send(IShellProvider shell, MoSyncProject project, ITargetPhone phone, File packageToSend, IProgressMonitor monitor)
			throws CoreException {
		if (phone instanceof AndroidTargetPhone) {
			AndroidTargetPhone androidPhone = (AndroidTargetPhone) phone;
			String serialNumberOfDevice = androidPhone.getSerialNumber();
			ADB.getDefault().install(packageToSend, serialNumberOfDevice);
			String androidComponent = getAndroidComponentName(project);
			ADB.getDefault().launch(androidComponent, serialNumberOfDevice);
		} else {
			throw new CoreException(new Status(IStatus.ERROR, TargetPhonePlugin.PLUGIN_ID, "Can only send to android phones"));
		}
	}
	
	public static String getAndroidComponentName(MoSyncProject project) {
		// Android app names are like this by convention:
		String packageName = "com.mosync.app_" + project.getName();
		String activityName = packageName + ".MoSync";
		String androidComponent = packageName + "/" + activityName;
		return androidComponent;
	}

	public String getDescription(String context) {
		return "Android USB";
	}

}
