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
package com.mobilesorcery.sdk.internal;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;

public interface PID extends StdCallLibrary {

    public final static String WIN32_LIB = "pid2";
    
    public final static PID INSTANCE = (PID) Native.loadLibrary(WIN32_LIB, PID.class);
    
    public int pid();
    
}