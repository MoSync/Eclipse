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
