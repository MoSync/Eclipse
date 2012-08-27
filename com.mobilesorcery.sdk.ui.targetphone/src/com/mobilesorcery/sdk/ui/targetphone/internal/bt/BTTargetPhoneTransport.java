package com.mobilesorcery.sdk.ui.targetphone.internal.bt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.dialogs.ListDialog;

import com.mobilesorcery.sdk.core.AbstractTool;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.Version;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhone;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhoneTransport;
import com.mobilesorcery.sdk.ui.targetphone.TargetPhonePlugin;

public class BTTargetPhoneTransport implements ITargetPhoneTransport {

	private final static Version OSX_108 = new Version("10.8");
	
	private static void assertAvailability() throws CoreException {
		if (AbstractTool.isMac()) {
			Version version = new Version(System.getProperty("os.version"));
			if (version.isValid() && !version.isOlder(OSX_108)) {
				throw new CoreException(new Status(IStatus.ERROR, TargetPhonePlugin.PLUGIN_ID,
						"BlueTooth is only supported for Mac OS X before 10.8"));
			}
		}
	}
	
	@Override
	public ITargetPhone load(IMemento memento, String name) {
		String addr = memento.getString("addr");
		Integer portInt = memento.getInteger("port");
		int port = portInt == null ? -1 : portInt.intValue();
		BTTargetPhone newPhone = new BTTargetPhone(name.toCharArray(), Util
				.fromBase16(addr), port);
		return newPhone;
	}

	@Override
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

	@Override
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
	public static BTTargetPhone selectPhone( final Shell shell ) throws IOException
	{
		// Use a native dialog on windows
		/*if( System.getProperty("os.name").toLowerCase().indexOf("win") != -1)
		{
			SearchBTDeviceDialog dialog = new SearchBTDeviceDialog( );
			BTTargetPhone info = dialog.open();
			return info;
		}*/

		final ArrayList<BTTargetPhone> result = new ArrayList<BTTargetPhone>( );

    	shell.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					BluetoothDialog ld = new BluetoothDialog( shell );
					ld.open( );

					if( ld.getReturnCode( ) == ListDialog.OK )
					{
						BluetoothDevice selectedDevice = ld.getSelectedDevice();
						BTTargetPhone targetDevice = selectedDevice.getTargetPhone();
						ITargetPhone correspondingDevice = findInHistory(targetDevice);
						if (correspondingDevice != null) {
							targetDevice.setPreferredProfile(MoSyncTool.DEFAULT_PROFILE_TYPE, correspondingDevice.getPreferredProfile(MoSyncTool.DEFAULT_PROFILE_TYPE));
							targetDevice.setPreferredProfile(MoSyncTool.LEGACY_PROFILE_TYPE, correspondingDevice.getPreferredProfile(MoSyncTool.LEGACY_PROFILE_TYPE));
						}
					    result.add(targetDevice);
					}
				}
				catch (Exception e) {
					e.printStackTrace( );
				}
			}
		});

    	if( result.size( ) > 0)
    	{
    		return result.get( 0 );
    	}
    	else
    	{
    		return null;
    	}
	}

	/**
	 * Returns the corresponding target phone from history - can be
	 * used to find out if there is already a preferred profile for a device
	 * @param btTargetPhone
	 * @return
	 */
	public static ITargetPhone findInHistory(BTTargetPhone btTargetPhone) {
        List<ITargetPhone> history = TargetPhonePlugin.getDefault().getSelectedTargetPhoneHistory();
        for (ITargetPhone targetPhone : history) {
            if (targetPhone instanceof BTTargetPhone) {
                if (btTargetPhone.getAddress().equals(((BTTargetPhone) targetPhone).getAddress())) {
                    return targetPhone;
                }
            }
        }

        return null;
    }
	@Override
	public void send(IShellProvider shell, MoSyncProject project,
			ITargetPhone phone, File packageToSend, IProgressMonitor monitor)
			throws CoreException {
		assertAvailability();
		BTSendJob job = new BTSendJob(shell, (BTTargetPhone) phone,
				packageToSend);
		job.runSync(monitor);
	}

	@Override
	public ITargetPhone scan(IShellProvider shell, IProgressMonitor monitor)
			throws CoreException {
		assertAvailability();
		try {
			BTTargetPhone phone = selectPhone( shell.getShell( ) );
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

	@Override
	public ImageDescriptor getIcon() {
		return MosyncUIPlugin.getDefault().getImageRegistry().getDescriptor(MosyncUIPlugin.PHONE_IMAGE);
	}

	@Override
	public String getDescription(String context) {
		return "Bluetooth";
	}

	@Override
	public boolean isAvailable() {
		return true;
	}

	@Override
	public IDeviceFilter getAcceptedProfiles() {
		return null;
	}

}
