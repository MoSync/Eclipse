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
