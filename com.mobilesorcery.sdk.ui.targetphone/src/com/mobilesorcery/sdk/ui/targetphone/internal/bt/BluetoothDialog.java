package com.mobilesorcery.sdk.ui.targetphone.internal.bt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListDialog;

/**
 * Pops up a dialog that can be used to select
 * a bluetooth device in a list of discovered
 * bluetooth devices.
 * 
 * @author fmattias
 */
public class BluetoothDialog
extends ListDialog
{
	public BluetoothDialog (Shell parent)
	{
		super( parent );
		setTitle( "Discover devices" );
	}

	/**
	 * This function starts the discovery for devices. The function
	 * starts the search in a new thread, and will return before
	 * all devices has been found.
	 */
	public void discoverDevices()
	{
		/* Get local dongle and set up an agent for device discovery */
		LocalDevice dongle = null;
		try
		{
			dongle = LocalDevice.getLocalDevice( );
		} 
		catch (BluetoothStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		DiscoveryAgent agent = dongle.getDiscoveryAgent( );
		
		/* Discover devices */
		BluetoothDeviceDiscoverer discoverer = new BluetoothDeviceDiscoverer( new UIUpdater( this ) );
		
		try
		{
			agent.startInquiry( DiscoveryAgent.GIAC, discoverer );
		} 
		catch ( BluetoothStateException e )
		{
			e.printStackTrace( );
		}
	}
	
	
	/**
	 * Handles the event of a device being discovered.
	 * 
	 * @author fmattias
	 */
	public class BluetoothDeviceDiscoverer implements DiscoveryListener
	{
		/**
		 * List of devices that has been discovered so far.
		 */
		private HashSet<BluetoothDevice> m_devices;
		
		/**
		 * Class called when a device is discovered.
		 */
		private DeviceUpdate m_updater;

		/**
		 * 
		 * @param updater Class that should be called when a device is found.
		 */
		public BluetoothDeviceDiscoverer(DeviceUpdate updater)
		{
			m_devices = new HashSet<BluetoothDevice>( 10 );
			m_updater = updater;
		}

		public void deviceDiscovered(RemoteDevice device, DeviceClass type) 
		{
			BluetoothDevice btDevice = new BluetoothDevice( device, type );
			
			/* Only add devices that are not available */
			if( !m_devices.contains( btDevice ) )
			{
				m_devices.add( btDevice );
				m_updater.deviceFound( btDevice );
			}			
		}

		public void inquiryCompleted(int arg0) 
		{	
		}

		public void serviceSearchCompleted(int arg0, int arg1) 
		{
		}

		public void servicesDiscovered(int arg0, ServiceRecord[] arg1) 
		{
		}
		
		public ArrayList<BluetoothDevice> getDevices()
		{
			return new ArrayList<BluetoothDevice>( m_devices );
		}
		
	}
	
	/**
	 * Interface used when a device is discovered. 
	 */
	public interface DeviceUpdate
	{
		/**
		 * Is called once a device is discovered.
		 * 
		 * @param device The devices that has been discovered.
		 */
		public void deviceFound( BluetoothDevice device );
	}
	
	/**
	 * Updates the UI as soon as a device is discovered.
	 */
	public class UIUpdater
	implements DeviceUpdate
	{
		/**
		 * Main widget that will be updated.
		 */
		ListDialog m_parent;
		
		/**
		 * A list of devices that will be shown in the UI.
		 */
		ArrayList<BluetoothDevice> m_devices;
		
		public UIUpdater(ListDialog parent)
		{
			m_devices = new ArrayList<BluetoothDevice>( );
			m_parent = parent;
		}

		/**
		 * 
		 */
		public void deviceFound(final BluetoothDevice device) 
		{	
			m_devices.add( device );
			m_parent.getShell( ).getDisplay( ).syncExec( new ListUpdater( m_parent, m_devices ) );
		}
		
		/**
		 * Class that updates the UI as a thread.
		 * 
		 * @author fmattias
		 */
		public class ListUpdater implements Runnable
		{
			ListDialog m_parent;
			List<BluetoothDevice> m_devices;
			
			/**
			 * Creates an UI update task that should update the
			 * list of devices.
			 * 
			 * @param parent The bluetooth dialog where the elements should
			 *               be shown.
			 * @param names A list of devices that should be shown in the dialog. It 
			 * 				is assumed that this list is thread safe.
			 */
			ListUpdater(ListDialog parent, List<BluetoothDevice> devices)
			{
				m_parent = parent;
				m_devices = devices;
			}
			
			public void run( ) 
			{
				// We need to access the inner table to update the list of devices.
				m_parent.getTableViewer().setInput( m_devices.toArray( ) );
			}
		}
	}
}
