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
import java.text.MessageFormat;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.sun.jna.Memory;

public class OSPipeInputStream extends InputStream {

    private int fd;
	
    private boolean closed = false;
    
    /**
     * @param fd The OS file descriptor
     */
    public OSPipeInputStream(int fd) {
        this.fd = fd;
    }
    
    /**
     * Assumes an open pipe
     */
    public int read(byte[] buffer, int offset, int length) throws IOException {
    	if (length <= 0) {
    		return 0;
    	}
    	
        Memory memory = new Memory(length);
        int read = CoreMoSyncPlugin.getDefault().getProcessUtil().pipe_read(fd, memory, length);
        
        if (read < 0) {
            throw new IOException(MessageFormat.format("Could not read from OS pipe [{0}]", fd));
        }
        
        if (read == 0) {
            return -1; // Java EOF
        }

        System.arraycopy(memory.getByteArray(0, length), 0, buffer, offset, length);        
        return read;
    }
    
    public int read() throws IOException {
        byte[] buffer = new byte[1];
        int read = read(buffer, 0, 1);
        if (read == -1) {
            return -1;
        } else {
            return buffer[0];
        }
    }
    
    public void close() {
    	if (!closed) {
    		closed = true;
    		CoreMoSyncPlugin.getDefault().getProcessUtil().pipe_close(fd);
    	}
    }
}
