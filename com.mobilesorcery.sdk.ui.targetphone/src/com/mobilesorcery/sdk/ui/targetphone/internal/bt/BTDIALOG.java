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

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public interface BTDIALOG extends Library {
    
    public final static String BTDIALOG_LIB = "btdialog";

    public final static BTDIALOG INSTANCE = (BTDIALOG) Native.loadLibrary(BTDIALOG_LIB, BTDIALOG.class);

    public final static int BTD_ERROR = 0;
    public final static int BTD_OK = 1;
    public final static int BTD_CANCEL = 2;
    
    //returns one of BtdResult.
    //fills device if BTD_OK is returned.
    int btDialog(Pointer deviceInfo);

}
