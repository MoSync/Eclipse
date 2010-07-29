package com.mobilesorcery.sdk.builder.s60;

import com.mobilesorcery.sdk.core.security.IApplicationPermissions;
import com.mobilesorcery.sdk.core.security.ICommonPermissions;

/**
 * A utility class for translating 
 * abstract permissions into Symbian Capabilities
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 *
 */
public class Capabilities {

    //public final static int ECapabilityTCB = 0;
    //public final static int ECapabilityCommDD = 1;
    //public final static int ECapabilityPowerMgmt = 2;
    //public final static int ECapabilityMultimediaDD = 3;
    //public final static int ECapabilityReadDeviceData = 4;
    //public final static int ECapabilityWriteDeviceData = 5;
    //public final static int ECapabilityDRM = 6;
    //public final static int ECapabilityTrustedUI = 7;
    //public final static int ECapabilityProtServ = 8;
    //public final static int ECapabilityDiskAdmin = 9;
    //public final static int ECapabilityNetworkControl = 10;
    //public final static int ECapabilityAllFiles = 11;
    //public final static int ECapabilitySwEvent = 12;
    public final static int ECapabilityNetworkServices = 13;
    public final static int ECapabilityLocalServices = 14;
    public final static int ECapabilityReadUserData = 15;
    public final static int ECapabilityWriteUserData = 16;
    public final static int ECapabilityLocation = 17;
    //public final static int ECapabilitySurroundingsDD = 18;
    public final static int ECapabilityUserEnvironment = 19;
    
    public static int toCapability(IApplicationPermissions permissions) {
        int capability = 0;
        /*
         * From the Symbian Wiki:
         * "The NetworkServices capability is required to use any sort of network protocol.
         * This includes making phone calls, sending email, sending MMS/SMS and using any 
         * IP based protocol (SIP, HTTP, FTP etc.)."
         */
        capability |= toCapability(permissions, ICommonPermissions.INTERNET, ECapabilityNetworkServices);
        capability |= toCapability(permissions, ICommonPermissions.SMS_READ, ECapabilityNetworkServices);
        capability |= toCapability(permissions, ICommonPermissions.SMS_RECEIVE, ECapabilityNetworkServices);
        capability |= toCapability(permissions, ICommonPermissions.SMS_SEND, ECapabilityNetworkServices);
        
        /*
         * From the Symbian Wiki:
         * "The LocalServices capability grants access to remote services in the close 
         * vicinity of the phone. The location of the remote service is well-known
         *  to the phone user. In most cases, such services will not incur any cost to the phone user.
         * The primary application for this capability is using the Bluetooth 
         * functionality of the phone and this capability is required to connect to or 
         * query information about another Bluetooth device."
         */
        capability |= toCapability(permissions, ICommonPermissions.BLUETOOTH, ECapabilityLocalServices);
        
        /*
         * From the Symbian Wiki:
         * "The Location capability protects APIs that reveal the location of the device, for example GPS.
         * [...]
         * early S60 3rd edition phones (pre FP2) don't allow installation of self-signed software 
         * with this capability."
         */
        capability |= toCapability(permissions, ICommonPermissions.LOCATION_COARSE, ECapabilityLocation);
        capability |= toCapability(permissions, ICommonPermissions.LOCATION_FINE, ECapabilityLocation);
        capability |= toCapability(permissions, ICommonPermissions.LOCATION, ECapabilityLocation);
        
        /*
         * From the Symbian Wiki:
         * Grants read access to data belonging to the phone user. This capability supports 
         * the confidentiality of user data. Contacts, messages and calendar data are always seen 
         * as user confidential data. For other content types such as images or sounds, 
         * it may depend on context, and ultimately be up to the application owning the data to define.
         */
        capability |= toCapability(permissions, ICommonPermissions.CALENDAR_READ, ECapabilityReadUserData);
        capability |= toCapability(permissions, ICommonPermissions.CONTACTS_READ, ECapabilityReadUserData);
        capability |= toCapability(permissions, ICommonPermissions.SMS_READ, ECapabilityReadUserData);
        
        /*
         * From the Symbian Wiki:
         * This capability protects access to certain data that is considered private to the phone 
         * user, i.e. contacts, calendar entries and messages. It supports the integrity of the user 
         * data, so that the user can be sure that it is correct. The application of this capability
         * is not necessarily symmetrical with the 'ReadUserData' capability
         */
        capability |= toCapability(permissions, ICommonPermissions.CALENDAR_WRITE, ECapabilityWriteUserData);
        capability |= toCapability(permissions, ICommonPermissions.CONTACTS_WRITE, ECapabilityWriteUserData);
        
        /*
         * From the Symbian Wiki:
         * "The UserEnvironment capability controls access to APIs that reveal information
         * about the environment of the device, for example the camera and microphone"
         */
        capability |= toCapability(permissions, ICommonPermissions.CAMERA, ECapabilityUserEnvironment);
        
        return capability;
    }

    private static int toCapability(IApplicationPermissions permissions, String requiredPermission, int capability) {
        if (permissions.isPermissionRequired(requiredPermission)) {
            return 1 << capability;
        }
        
        return 0;
    }
}
