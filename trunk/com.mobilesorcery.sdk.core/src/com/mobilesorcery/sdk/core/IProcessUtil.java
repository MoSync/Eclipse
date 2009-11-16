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
package com.mobilesorcery.sdk.core;

import com.sun.jna.Pointer;

public interface IProcessUtil {
    
    public void pipe_create(int[] fds);
    
    public int pipe_read(int fd, Pointer buffer, int length);
    
    public int pipe_write(int fd, Pointer buffer, int length);

    public void pipe_close(int fd);
    
    public int pipe_dup(int fd);

    public void pipe_dup2(int fd1, int fd2);
    
    public int proc_spawn(byte[] path, byte[] params, byte[] dir);
    
    public int proc_wait_for(int handle);
    
    public int proc_kill(int pid, int exitcode);
}
