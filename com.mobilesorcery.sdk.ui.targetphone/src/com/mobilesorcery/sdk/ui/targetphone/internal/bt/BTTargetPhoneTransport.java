package com.mobilesorcery.sdk.ui.targetphone.internal.bt;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.profiles.ui.Activator;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhone;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhoneTransport;
import com.mobilesorcery.sdk.ui.targetphone.TargetPhonePlugin;

public class BTTargetPhoneTransport implements ITargetPhoneTransport {

	public ITargetPhone load(IMemento memento, String name) {
		String addr = memento.getString("addr");
		Integer portInt = memento.getInteger("port");
		int port = portInt == null ? -1 : portInt.intValue();
		BTTargetPhone newPhone = new BTTargetPhone(name.toCharArray(), Util
				.fromBase16(addr), port);
		return newPhone;
	}

	public boolean store(ITargetPhone aPhone, IMemento memento) {
		if (aPhone instanceof BTTargetPhone) {
			BTTargetPhone phone = (BTTargetPhone) aPhone;
			String addr = Util.toBase16(phone.getAddressAsBytes());
			int port = phone.getPort();

			memento.putString("addr", addr);
			memento.putInteger("port", phone.getPort());
			return true;
		} else {
			return false;
		}
	}

	public String getId() {
		return TargetPhonePlugin.DEFAULT_TARGET_PHONE_TRANSPORT;
	}

	public static IStatus assignPort(IShellProvider shellProvider,
			IProgressMonitor monitor, BTTargetPhone phone) {
		try {
			monitor.beginTask("Scanning BT device for OBEX service", 1);
			monitor.setTaskName("Scanning BT device for OBEX service");
			int port = ServiceSearch.search(phone.getAddress());
			if (port != -1) {
				phone.assignPort(port);
			} else {
				return new Status(IStatus.ERROR, TargetPhonePlugin.PLUGIN_ID,
						"The device connected to has no OBEX service");
			}

			return Status.OK_STATUS;
		} catch (Exception e) {
			return new Status(IStatus.ERROR, TargetPhonePlugin.PLUGIN_ID,
					"The device connected to has no OBEX service", e);
		} finally {
			monitor.worked(1);
		}
	}

	/**
	 * Pops up a dialog where the user can select a [BT] target phone.
	 * 
	 * @return
	 * @throws IOException
	 */
	public static BTTargetPhone selectPhone() throws IOException {
		SearchBTDeviceDialog dialog = new SearchBTDeviceDialog();
		BTTargetPhone info = dialog.open();
		return info;
	}

	public void send(IShellProvider shell, MoSyncProject project,
			ITargetPhone phone, File packageToSend, IProgressMonitor monitor)
			throws CoreException {
		BTSendJob job = new BTSendJob(shell, (BTTargetPhone) phone,
				packageToSend);
		job.runSync(monitor);
	}

	public ITargetPhone scan(IShellProvider shell, IProgressMonitor monitor)
			throws CoreException {
		try {
			BTTargetPhone phone = selectPhone();
			if (phone != null) {
				IStatus status = assignPort(shell, monitor, phone);
				if (status.getSeverity() == IStatus.ERROR) {
					throw new CoreException(status);
				}
			}
			return phone;
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					TargetPhonePlugin.PLUGIN_ID, e.getMessage(), e));
		}
	}

	public ImageDescriptor getIcon() {
		return Activator.getDefault().getImageRegistry().getDescriptor(Activator.PHONE_IMAGE);
	}

	public String getDescription(String context) {
		return "Bluetooth";
	}

}
