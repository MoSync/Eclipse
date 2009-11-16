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
package com.mobilesorcery.sdk.ui.targetphone.internal;

import java.io.IOException;

import com.sun.jna.Memory;
import com.sun.jna.NativeLibrary;

public class SearchDeviceDialog {

    private final static int ADDRESS_SIZE = 6;
    private final static int NAME_LENGTH = 248;

    private final static int SIZEOF_SHORT = 2;

    public SearchDeviceDialog() {
        
    }
    
    public TargetPhone open() throws IOException {
        int size = ADDRESS_SIZE + NAME_LENGTH * SIZEOF_SHORT;
        Memory deviceInfo = new Memory(size); // Will be GC'd.
        int result = BTDIALOG.BTD_ERROR;
        
        try {
            NativeLibrary.getInstance(BTDIALOG.WIN32_LIB).getFunction("btDialog");
            result = BTDIALOG.INSTANCE.btDialog(deviceInfo);
        } catch (Throwable e) {
            throw new IOException(e);
        }

        switch (result) {
            case BTDIALOG.BTD_OK:
                byte[] addr = deviceInfo.getByteArray(0, ADDRESS_SIZE);
                char[] name = deviceInfo.getCharArray(ADDRESS_SIZE, NAME_LENGTH);
                
                return new TargetPhone(name, addr, TargetPhone.PORT_UNASSIGNED);
            case BTDIALOG.BTD_ERROR:
                throw new IOException("General bluetooth dialog error");
            default:
                return null;
        }

    }
}
