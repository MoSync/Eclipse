package com.mobilesorcery.sdk.internal;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;

public interface PID extends StdCallLibrary {

    public final static String WIN32_LIB = "pid2";
    
    public final static PID INSTANCE = (PID) Native.loadLibrary(WIN32_LIB, PID.class);
    
    public int pid();
    
}