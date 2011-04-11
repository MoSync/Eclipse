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

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;

public class LineReader implements Runnable {

    public interface ILineHandler {
    	public void start(Process process);
        public void newLine(String line);
		/**
		 * <p>Is called upon when either<p>
		 * <ul>
		 * <li>An exception occurs, or
		 * <li>EOF
		 * </ul>
		 * @param e The exception that caused this process to exit (or not start), or <code>null</code> if EOF.
		 */
        public void stop(IOException e);
    }
    
    public static class LineAdapter implements ILineHandler {
    	public void start(Process process) {
    		
    	}
		public void newLine(String line) {
		}

		public void stop(IOException e) {
		}    	
    }

    private Reader from;
    private ILineHandler callback;
    
    public LineReader(Reader from, ILineHandler callback) {
        this.from = from;
        this.callback = callback;
    }
    
    public void start() {
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }
    
    public void run() {
        try {
            LineNumberReader reader = new LineNumberReader(from);
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (callback != null) {
                	callback.newLine(line);
                }
            }
            
            if (callback != null) {
            	callback.stop(null);	
            }
            
        } catch (IOException e) {
        	if (callback != null) {
        		callback.stop(e);
        	}
            e.printStackTrace();
        }
    }
}
