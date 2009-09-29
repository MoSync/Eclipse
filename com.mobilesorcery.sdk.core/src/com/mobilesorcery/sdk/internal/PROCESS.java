package com.mobilesorcery.sdk.internal;

import com.mobilesorcery.sdk.core.IProcessUtil;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;

public interface PROCESS extends StdCallLibrary, IProcessUtil {

    public final static String WIN32_LIB = "pipelib";       

    final static PROCESS INSTANCE = (PROCESS) Native.loadLibrary(WIN32_LIB, PROCESS.class);

}
