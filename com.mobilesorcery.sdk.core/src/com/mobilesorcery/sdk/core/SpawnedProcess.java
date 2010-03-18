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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

import org.eclipse.core.runtime.ListenerList;


public class SpawnedProcess extends Process {

    public interface IProcessListener {

    	public final static int START = 0;
    	public final static int EXIT = 1;
    	public final static int DESTROY_CALLED = 2;
    	
    	public void handleEvent(SpawnedProcess process, int type);
    	
	}

	private static final int INITIAL_EXIT_VALUE = Integer.MIN_VALUE;
    
	private String cmd;
    private String args;
    private File dir;

    private int handle = 0;

    private int exitValue = INITIAL_EXIT_VALUE;

    private InputStream errStream = new ByteArrayInputStream(new byte[0]);
    private InputStream inpStream = new ByteArrayInputStream(new byte[0]);

    private OutputStream outputStream = new ByteArrayOutputStream();

    private CountDownLatch waitFor;
    
    private ListenerList processListeners = new ListenerList();

	private Runnable shutdownHook = null;
    
    public SpawnedProcess(String cmd, String args, File dir) {
        waitFor = new CountDownLatch(1);
        this.cmd = cmd;
        this.args = args;
        this.dir = dir;
    }

    public void start() {
    	notifyListeners(IProcessListener.START);
    	final IProcessUtil pu = CoreMoSyncPlugin.getDefault().getProcessUtil();
    	
        handle = pu.proc_spawn((cmd + '\0').getBytes(), (args + '\0').getBytes(), (dir.getAbsolutePath() + '\0')
                .getBytes());
        
        Thread waitForThread = new Thread(new Runnable() {
            public void run() {
                int tmpExitValue = pu.proc_wait_for(handle);
            	runShutdownHook();
            	exitValue = tmpExitValue;
                notifyListeners(IProcessListener.EXIT);
                waitFor.countDown();
            }            
        });
        
        waitForThread.setName("Waiting for process " + cmd);
        waitForThread.setDaemon(true);
        waitForThread.start();
        
        if (handle < 0) {
            throw new IllegalStateException("Could not spawn process");
        }
    }
    
    public static int[] createPipe() {
        int[] fds = new int[2];
        CoreMoSyncPlugin.getDefault().getProcessUtil().pipe_create(fds);
        return fds;
    }

    public static void close(int fd) {
    	CoreMoSyncPlugin.getDefault().getProcessUtil().pipe_close(fd);
    }

    public void setErrorStream(InputStream errStream) {
        if (errStream != null) {
            this.errStream = errStream;
        }
    }

    public void setInputStream(InputStream inpStream) {
        if (inpStream != null) {
            this.inpStream = inpStream;
        }
    }

    public void setOutputFd(int outputFd) {
        throw new UnsupportedOperationException("Only need read in this app");
    }

    public void destroy() {
    	notifyListeners(IProcessListener.DESTROY_CALLED);
    	
    	if (exitValue != INITIAL_EXIT_VALUE) {
    		closeStreams();    		
    		return;
    	}

    	// TODO: There is no properly implemented process destroy
        // Will need to use CreateProcess, TerminateProcess, etc instead
        // of _spawn, _cwait, etc.
        // OR - if a debug interface is implemented for the emulator...?
    	int result = CoreMoSyncPlugin.getDefault().getProcessUtil().proc_kill(handle, 0);
        /*if (result != 0) {
            throw new IllegalStateException("Could not terminate application");
        }*/
    }

    private void closeStreams() {
    	if (outputStream != null) {
    		try {
				outputStream.close();
			} catch (IOException e) {
				// Ignore.
			}
    	}
    	
    	if (errStream != null) {
    		try {
				errStream.close();
			} catch (IOException e) {
				// Ignore.
			}
    	}
    	
    	if (inpStream != null) {
    		try {
				inpStream.close();
			} catch (IOException e) {
				// Ignore.
			}
    	}
	}

	public int exitValue() {
        if (exitValue == INITIAL_EXIT_VALUE) {
            throw new IllegalThreadStateException("Process not terminated");
        }
        return exitValue;
    }

    public InputStream getErrorStream() {
        return errStream;
    }

    public InputStream getInputStream() {
        return inpStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void setShutdownHook(Runnable shutdownHook) {
    	this.shutdownHook = shutdownHook;
    }
    
    private void runShutdownHook() {
    	if (shutdownHook != null) {
    		shutdownHook.run();
    	}
	}

	public String toString() {
        return "[" + dir + ":] " + cmd + " " + args;
    }

    public int waitFor() throws InterruptedException {
        waitFor.await();
        return exitValue;
    }

    public void addProcessListener(IProcessListener listener) {
    	processListeners.add(listener);
    }
    
    public void removeProcessListener(IProcessListener listener) {
    	processListeners.remove(listener);
    }
    
    protected void notifyListeners(int type) {
    	Object[] listeners = processListeners.getListeners();
    	for (int i = 0; i < listeners.length; i++) {
    		((IProcessListener) listeners[i]).handleEvent(this, type);
    	}
    }
}
