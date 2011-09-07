/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.core.security;

public interface ICommonPermissions {

    final static String READ = "/Read";
    final static String WRITE = "/Write";

    public final static String BLUETOOTH = "Bluetooth";

    public final static String CALENDAR = "Calendar";
    public final static String CALENDAR_READ = CALENDAR + READ;
    public final static String CALENDAR_WRITE = CALENDAR + WRITE;

    public final static String CAMERA = "Camera";

    public final static String CONTACTS = "Contacts";
    public final static String CONTACTS_READ = CONTACTS + READ;
    public final static String CONTACTS_WRITE = CONTACTS + WRITE;

    public final static String FILE_STORAGE = "File Storage";
    public final static String FILE_STORAGE_WRITE = FILE_STORAGE + WRITE;
    public final static String FILE_STORAGE_READ = FILE_STORAGE + READ;
    //public final static String FILE_STORAGE_SEEK = FILE_STORAGE + "/Seek";

    public final static String INTERNET = "Internet Access";

    public final static String LOCATION = "Location";
    public final static String LOCATION_FINE = LOCATION + "/Fine";
    public final static String LOCATION_COARSE = LOCATION + "/Coarse";

    public final static String POWER_MANAGEMENT = "Power Management";

    public final static String SMS = "SMS";
    public final static String SMS_SEND = SMS + "/Send";
    public final static String SMS_RECEIVE = SMS + "/Receive";
    public final static String SMS_READ = SMS + "/Read";

    public final static String VIBRATE = "Vibrate";

    public final static String HOMESCREEN = "Home Screen";

    public final static String AUTOSTART = "Auto Start";

	public static final String NFC = "NFC";

    public final static String[] ALL_PERMISSIONS = new String[] {
        BLUETOOTH,
        CALENDAR, CALENDAR_READ, CALENDAR_WRITE,
        CAMERA,
        CONTACTS, CONTACTS_READ, CONTACTS_WRITE,
        FILE_STORAGE, FILE_STORAGE_READ, FILE_STORAGE_WRITE,
        INTERNET,
        LOCATION, LOCATION_FINE, LOCATION_COARSE,
        POWER_MANAGEMENT,
        SMS, SMS_SEND, SMS_RECEIVE, SMS_READ,
        VIBRATE,
		HOMESCREEN,
		AUTOSTART/*,
		NFC*/
    };
}
