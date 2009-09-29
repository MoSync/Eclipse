package com.mobilesorcery.sdk.ui.targetphone.internal;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;

public interface BTDIALOG extends Library {
    
    public final static String WIN32_LIB = "btDialog";

    public final static BTDIALOG INSTANCE = (BTDIALOG) Native.loadLibrary(WIN32_LIB, BTDIALOG.class);

    public final static int BTD_ERROR = 0;
    public final static int BTD_OK = 1;
    public final static int BTD_CANCEL = 2;
    
    //returns one of BtdResult.
    //fills device if BTD_OK is returned.
    int btDialog(Pointer deviceInfo);

}
