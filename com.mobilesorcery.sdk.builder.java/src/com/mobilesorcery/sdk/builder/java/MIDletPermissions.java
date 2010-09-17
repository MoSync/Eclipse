package com.mobilesorcery.sdk.builder.java;

import java.util.Arrays;
import java.util.Set;

import com.mobilesorcery.sdk.core.security.IApplicationPermissions;
import com.mobilesorcery.sdk.core.security.ICommonPermissions;

public class MIDletPermissions {

    public static void toMIDletPermissions(IApplicationPermissions inPermissions, Set<String> outPermissions, Set<String> outOptPermissions) {
        // Bluetooth
        addMIDletPermissions(outPermissions, inPermissions.isPermissionRequested(ICommonPermissions.BLUETOOTH), 
                "javax.microedition.io.Connector.bluetooth.client",
                "javax.microedition.io.Connector.bluetooth.server");
        
        // Calendar
        addMIDletPermissions(outPermissions, inPermissions.isPermissionRequested(ICommonPermissions.CALENDAR_READ),
                "javax.microedition.pim.EventList.read",
                "javax.microedition.pim.ToDoList.read");
        addMIDletPermissions(outPermissions, inPermissions.isPermissionRequested(ICommonPermissions.CALENDAR_WRITE),
                "javax.microedition.pim.EventList.write",
                "javax.microedition.pim.ToDoList.write");
        
        // Camera -- req. signing
        addMIDletPermissions(outOptPermissions, inPermissions.isPermissionRequested(ICommonPermissions.CAMERA),
                "javax.microedition.media.control.VideoControl.getSnapshot");
        
        // Contacts
        addMIDletPermissions(outPermissions, inPermissions.isPermissionRequested(ICommonPermissions.CONTACTS_READ),
            "javax.microedition.pim.ContactList.read");
        addMIDletPermissions(outPermissions, inPermissions.isPermissionRequested(ICommonPermissions.CONTACTS_WRITE),
                "javax.microedition.pim.ContactList.write");
        
        // File storage
        addMIDletPermissions(outPermissions, inPermissions.isPermissionRequested(ICommonPermissions.FILE_STORAGE_READ),
                "javax.microedition.io.Connector.file.read");
        addMIDletPermissions(outPermissions, inPermissions.isPermissionRequested(ICommonPermissions.FILE_STORAGE_WRITE),
                "javax.microedition.io.Connector.file.write");
    
        // Internet & networking
        addMIDletPermissions(outPermissions, inPermissions.isPermissionRequested(ICommonPermissions.INTERNET),
        "javax.microedition.io.Connector.http");
        addMIDletPermissions(outOptPermissions, inPermissions.isPermissionRequested(ICommonPermissions.INTERNET),
                "javax.microedition.io.Connector.socket");

        // Power mgmt - no permissions
        
        // SMS & Messaging
        addMIDletPermissions(outPermissions, inPermissions.isPermissionRequested(ICommonPermissions.SMS_RECEIVE),
                "javax.microedition.io.Connector.sms",
                "javax.wireless.messaging.mms.receive");
        addMIDletPermissions(outPermissions, inPermissions.isPermissionRequested(ICommonPermissions.SMS_SEND),
                "javax.microedition.io.Connector.sms",
                "javax.wireless.messaging.mms.send");
        
        // Vibrate - no permissions
        
        // Location
        addMIDletPermissions(outPermissions, anyOf(inPermissions, ICommonPermissions.LOCATION_FINE, ICommonPermissions.LOCATION_COARSE),
                "javax.microedition.location.Location",
                "javax.microedition.location.Orientation");
        
        
    }
    
    private static boolean anyOf(IApplicationPermissions permissions, String... commonPermissions) {
        for (int i = 0; i < commonPermissions.length; i++) {
            if (permissions.isPermissionRequested(commonPermissions[i])) {
                return true;
            }
        }
        
        return false;
    }

    private static void addMIDletPermissions(Set<String> result, boolean condition, String... permissions) {
        if (condition) {
            result.addAll(Arrays.asList(permissions));
        }
    }

}
