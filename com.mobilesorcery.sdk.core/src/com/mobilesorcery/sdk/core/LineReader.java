package com.mobilesorcery.sdk.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.Reader;

public class LineReader implements Runnable {

    public interface ILineHandler {
        public void newLine(String line);
        public void stop(IOException e);
    }
    
    public static class LineAdapter implements ILineHandler {
		public void newLine(String line) {
		}

		/**
		 * <p>Is called upon when either<p>
		 * <ul>
		 * <li>An exception occurs, or
		 * <li>EOF
		 * </ul>
		 */
		public void stop(IOException e) {
		}    	
    }

    private Reader from;
    private ILineHandler callback;

    private LineReader() {
        
    }
    
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
                callback.newLine(line);
            }
            
            callback.stop(null);
        } catch (IOException e) {
        	callback.stop(e);
            e.printStackTrace();
        }
    }
}
