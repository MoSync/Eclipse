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
package com.mobilesorcery.sdk.ui.targetphone.internal.bt;

import java.io.IOException;

import com.sun.jna.Memory;

public class SearchBTDeviceDialog {

    private final static int ADDRESS_SIZE = 6;
    private final static int NAME_LENGTH = 248;

    private final static int SIZEOF_SHORT = 2;

    public SearchBTDeviceDialog() {
        
    }
    
    public BTTargetPhone open() throws IOException {
        int size = ADDRESS_SIZE + NAME_LENGTH * SIZEOF_SHORT;
        Memory deviceInfo = new Memory(size); // Will be GC'd.
        int result = BTDIALOG.BTD_ERROR;
        
        try {
            result = BTDIALOG.INSTANCE.btDialog(deviceInfo);
        } catch (Throwable e) {
            throw new IOException(e);
        }

        switch (result) {
            case BTDIALOG.BTD_OK:
                byte[] addr = deviceInfo.getByteArray(0, ADDRESS_SIZE);
                char[] name = deviceInfo.getCharArray(ADDRESS_SIZE, NAME_LENGTH);
                
                return new BTTargetPhone(name, addr, BTTargetPhone.PORT_UNASSIGNED);
            case BTDIALOG.BTD_ERROR:
                throw new IOException("General bluetooth dialog error");
            default:
                return null;
        }

    }
}
