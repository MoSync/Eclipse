package com.mobilesorcery.sdk.ui.targetphone.internal.bt;

import java.io.IOException;
import java.util.HashMap;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.ui.IconAndMultilineLabelProvider;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhone;
import com.mobilesorcery.sdk.ui.targetphone.TargetPhonePlugin;

/**
 * Provides the UI with icons and a name of bluetooth devices.
 *
 * @author fmattias
 */
public class BluetoothLabelProvider extends IconAndMultilineLabelProvider {

	/**
	 * Maps different device types to different icons.
	 */
	HashMap<BluetoothDevice.Type, Image> m_icons = new HashMap<BluetoothDevice.Type, Image>();

	/**
	 * Initializes the icons shown for each device type. Will fail if the icons
	 * cannot be found.
	 *
	 * @param display
	 *            The display that handles the UI.
	 */
	public BluetoothLabelProvider(TableViewer viewer) {
		super(viewer);

		/* Find icons and map them to different device types. */
		try {
			m_icons.put(BluetoothDevice.Type.COMPUTER, new Image(display,
					getClass().getResource("/icons/desktop.png").openStream()));
			m_icons.put(BluetoothDevice.Type.LAPTOP, new Image(display,
					getClass().getResource("/icons/laptop.png").openStream()));
			m_icons.put(BluetoothDevice.Type.PHONE, new Image(display,
					getClass().getResource("/icons/phone.png").openStream()));
			m_icons.put(BluetoothDevice.Type.UNKNOWN,
					m_icons.get(BluetoothDevice.Type.COMPUTER));
		} catch (IOException e) {
			CoreMoSyncPlugin
					.getDefault()
					.getLog()
					.log(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID,
							"Could not load vital resources."));
		}
	}

	/**
	 * Returns the image corresponding to the given device, or null if the
	 * element is not a BluetoothDevice.
	 *
	 * @param element
	 *            A BluetoothDevice.
	 * @return The image corresponding to given element.
	 */
	@Override
	public Image getImage(Object element) {
		BluetoothDevice device;
		if (!(element instanceof BluetoothDevice)) {
			return null;
		}

		device = (BluetoothDevice) element;

		return m_icons.get(device.getType());
	}

	@Override
	public String[] getLines(Object element) {
		if (element instanceof BluetoothDevice) {
			BluetoothDevice btd = (BluetoothDevice) element;
			String name = btd.getProperty("name");
			String addr = btd.getProperty("address");
			ITargetPhone inHistory = BTTargetPhoneTransport.findInHistory(btd
					.getTargetPhone());
			int profileManagerType = TargetPhonePlugin.getDefault()
					.getCurrentProfileManagerType();
			IProfile preferredProfile = inHistory == null ? null : inHistory
					.getPreferredProfile(profileManagerType);
			String preferredProfileName = preferredProfile == null ? null
					: preferredProfile.getVendor() + " - "
							+ preferredProfile.getName();
			return new String[] { name, preferredProfileName, addr };
		}

		return new String[3];
	}

	@Override
	public void dispose() {
		super.dispose();
		for (Image icon : m_icons.values()) {
			icon.dispose();
		}
	}

}