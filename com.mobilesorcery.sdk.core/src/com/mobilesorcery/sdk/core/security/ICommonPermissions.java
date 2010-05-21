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

    // TODO: Find list of proper permissions
    public final static String INTERNET = "Internet Access";
    public final static String FILE_WRITE = "File System Access (Write)";
    public final static String FILE_READ = "File System Access (Read)";
    public final static String FILE_SEEK = "File System Access (Seek)";
    public final static String VIBRATE = "Vibrate";
    
    public final static String[] ALL_PERMISSIONS = new String[] { INTERNET, FILE_WRITE, FILE_READ, FILE_SEEK, VIBRATE };
    
}
