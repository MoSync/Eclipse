package com.mobilesorcery.sdk.ui.targetphone.internal.bt;

import java.io.IOException;
import java.util.HashMap;

import javax.bluetooth.DeviceClass;
import javax.bluetooth.RemoteDevice;

/**
 * Represents a single bluetooth device that has been discovered.
 * 
 * @author fmattias
 */
public class BluetoothDevice
{
	/**
	 * Returns the type of this bluetooth device, i.e. whether
	 * it is a phone, laptop or a stationary computer.
	 * @author fmattias
	 *
	 */
	public enum Type {
		COMPUTER, /* Computer */
		LAPTOP,   /* Laptop */
		PHONE,    /* Mobile phone */
		UNKNOWN
	};
	
	/** 
	 * A map from a property name to a property. 
	 */
	private HashMap<String, String> m_properties = new HashMap<String, String>();
	
	/**
	 * The type of the device, i.e. laptop, phone or computer.
	 */
	private Type m_deviceType;
	
	private static final int ADDRESS_LENGTH = 6;
	
	
	/**
	 * Creates a device with the given properties.
	 * 
	 * @param name
	 * @param address
	 * @param deviceType
	 */
	public BluetoothDevice(String name, String address, Type deviceType) 
	{
		m_properties.put( "name", name );
		m_properties.put( "address", address );
		m_deviceType = deviceType;
	}
	
	/**
	 * Creates a device from a discovered device. The name of the device
	 * will be resolved in this function.
	 * 
	 * @param device A device that has been discovered.
	 * @param deviceClass The type of the device, i.e. the major and minor
	 *                    device class of the bluetooth protocol.
	 */
	public BluetoothDevice(RemoteDevice device, DeviceClass deviceClass)
	{
		String name = "";
		
		try
		{
			name = device.getFriendlyName( true );
		}
		catch( IOException  e )
		{
			name = "Unknown";
		}
		
		m_properties.put( "name", name );
		m_properties.put( "address", device.getBluetoothAddress( ) );
		m_deviceType = getType( deviceClass.getMajorDeviceClass( ), deviceClass.getMinorDeviceClass( ) );
	}
	
	/**
	 * Converts the given minor and major device
	 * class to the Type enum.
	 * 
	 * @param majorClass The major class of the device (see bluetooth spec).
	 * @param minorClass The minor class of the device (see bluetooth spec).
	 * 
	 * @return The classes converted to an enum.
	 */
	private Type getType(int majorClass, int minorClass)
	{
		/* Find type of device. */
		if( majorClass == 0x100 )
		{
			if( minorClass == 0xC )
			{
				return Type.LAPTOP;
			}
			else
			{
				return Type.COMPUTER;
			}
		}
		else if( majorClass == 0x200 )
		{
			return Type.PHONE;
		}
		
		return Type.UNKNOWN;
	}
	
	/**
	 * Returns the value corresponding to name.
	 * 
	 * Currently these properties are supported:
	 * name - Returns the name of the device.
	 * address - Returns the mac address of the device.
	 * 
	 * @param name
	 * @return
	 */
	public String getProperty(String name)
	{
		return m_properties.get( name );
	}
	
	/**
	 * Returns the device class of this device. I.e. if it is a laptop,
	 * computer or phone.
	 * 
	 * @return The device class of this device.
	 */
	public Type getType()
	{
		return m_deviceType;
	}
	
	/**
	 * Converts the current device structure to the one
	 * used in btDialog.
	 * 
	 * Note that the port will always be unassinged.
	 * 
	 * @return the current device as a BTTargetPhone.
	 */
	public BTTargetPhone getTargetPhone()
	{
		char name[] = m_properties.get( "name" ).toCharArray( );
		byte addr[] = new byte[ADDRESS_LENGTH];
	
		/* Parse one byte at a time in the address, i.e. 2 hex chars. */
		String addrStr = m_properties.get( "address" );
		for(int i = 0; i < addrStr.length( ); i += 2)
		{
			int byteIndex = i / 2;
			String byteStr = addrStr.substring(i, i+2); // Not inclusive
			
			/* Convert from hex string to byte */
			addr[ byteIndex ] = (byte) Integer.parseInt(byteStr, 16);
		}
		
		// Set the port to unassigned and let it be discovered when scanning for OBEX.
		return new BTTargetPhone( name, addr, BTTargetPhone.PORT_UNASSIGNED );
	}
}
