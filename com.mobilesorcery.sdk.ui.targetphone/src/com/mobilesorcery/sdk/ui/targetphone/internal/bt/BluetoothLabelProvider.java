package com.mobilesorcery.sdk.ui.targetphone.internal.bt;

import java.io.IOException;
import java.util.HashMap;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;

/**
 * Provides the UI with icons and a name of bluetooth devices.
 * 
 * @author fmattias
 */
public class BluetoothLabelProvider extends LabelProvider
{
	/**
	 * Maps different device types to different icons.
	 */
	HashMap< BluetoothDevice.Type, Image> m_icons = new HashMap<BluetoothDevice.Type, Image>( );
	
	/**
	 * Initializes the icons shown for each device type. Will fail if the icons
	 * cannot be found.
	 * 
	 * @param display The display that handles the UI.
	 */
	public BluetoothLabelProvider(Display display)
	{
		
		/* Find icons and map them to different device types. */
		try {
			m_icons.put( BluetoothDevice.Type.COMPUTER, new Image( display, getClass( ).getResource( "/icons/desktop.png" ).openStream( ) ) );
			m_icons.put( BluetoothDevice.Type.LAPTOP, new Image( display, getClass( ).getResource( "/icons/laptop.png" ).openStream( ) ) );
			m_icons.put( BluetoothDevice.Type.PHONE, new Image( display, getClass( ).getResource( "/icons/phone.png" ).openStream( ) ) );
			m_icons.put( BluetoothDevice.Type.UNKNOWN, m_icons.get( BluetoothDevice.Type.COMPUTER ) );
		}
		catch( IOException e )
		{
			CoreMoSyncPlugin.getDefault().getLog().log( 
					new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, "Could not load vital resources.") );
		}
	}
	
	/**
	 * Returns the image corresponding to the given device, or null
	 * if the element is not a BluetoothDevice. 
	 * 
	 * @param element A BluetoothDevice.
	 * @return The image corresponding to given element. 
	 */
	public Image getImage(Object element) 
	{
		BluetoothDevice device;
		if( ! (element instanceof BluetoothDevice) )
		{
			return null;
		}
		
		device = (BluetoothDevice) element;
		
		return m_icons.get( device.getType( ) );
	}

	/**
	 * Shows the name and address of the given device.
	 * 
	 * @param element A BluetoothDevice.
	 * @return The name and address of the given bluetooth device, or
	 *         "Unknown" if the given element is not a BluetoothDevice.
	 */
	public String getText(Object element) 
	{
		BluetoothDevice device;
		if( ! (element instanceof BluetoothDevice) )
		{
			return "Unknown";
		}
		device = (BluetoothDevice) element;
		
		return device.getProperty( "name" ) + "\n" + device.getProperty( "address" );	
	}
}