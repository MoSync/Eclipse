package com.mobilesorcery.sdk.ui.targetphone.internal.bt;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;

import javax.bluetooth.DeviceClass;
import javax.bluetooth.RemoteDevice;

import com.mobilesorcery.sdk.core.SimpleQueue;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.ui.targetphone.internal.bt.BluetoothDialog.DeviceUpdate;

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
        PHONE,    /* Mobile phone */
		LAPTOP,   /* Laptop */
        COMPUTER, /* Computer */
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

    private RemoteDevice m_device;

    protected long m_getDiscoveryTimestamp;
	
	private static final int ADDRESS_LENGTH = 6;

    public static final Comparator<BluetoothDevice> COMPARATOR = new Comparator<BluetoothDevice>() {
        public int compare(BluetoothDevice device1, BluetoothDevice device2) {
            int result = device1.m_deviceType.compareTo(device2.m_deviceType);
            if (result == 0) {
                result = new Long(device1.m_getDiscoveryTimestamp).compareTo(device2.m_getDiscoveryTimestamp);
                if (result == 0) {
                    return new Integer(System.identityHashCode(device1)).compareTo(System.identityHashCode(device2));
                }
            }
            
            return result;
        }
    };
	
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
	public BluetoothDevice(final RemoteDevice device, DeviceClass deviceClass)
	{
	    // Ok, let's just for now assume creation time = discovery time...
	    m_getDiscoveryTimestamp = System.currentTimeMillis();
	    m_properties.put( "name", "");
		m_properties.put( "address", device.getBluetoothAddress( ) );
		m_device = device;
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

    public void resolveFriendlyName(DeviceUpdate updater) {
        // Just set the name temporarily
        String name = "Querying device for name..."; 
        m_properties.put( "name" , name);
        updater.deviceUpdated(this);
        try {
            name = m_device.getFriendlyName(false);
            // There seems to be a bug here: the first time
            // empty strings are returned from getFriendlyName(false)!?
            if (Util.isEmpty(name)) { 
                name = m_device.getFriendlyName(true);
            }
        } catch (IOException e) {
            name = "Could not resolve device name";
        }
        
        m_properties.put( "name", name);
        updater.deviceUpdated(this);
    }
}
