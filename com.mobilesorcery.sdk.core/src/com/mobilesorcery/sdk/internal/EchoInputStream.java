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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class EchoInputStream extends InputStream {

    private OutputStream echoTo;
    private InputStream echoThis;
    
    public EchoInputStream(InputStream echoThis, OutputStream echoTo) {
        this.echoThis = echoThis;
        this.echoTo = echoTo;
    }
    
    public int read() throws IOException {
        int read = echoThis.read();
        if (read != -1) {
            echoTo.write(read);
        }
        
        return read;
    }

    
}
