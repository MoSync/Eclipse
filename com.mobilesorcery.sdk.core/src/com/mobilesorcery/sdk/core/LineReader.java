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
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

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
    	@Override
		public void start(Process process) {

    	}
		@Override
		public void newLine(String line) {
		}

		@Override
		public void stop(IOException e) {
		}
    }
    
    /**
     * Allows for several {@link ILineHandler}s to listen
     * to the same source.
     *
     */
    public static class LineHandlerList implements ILineHandler {

    	CopyOnWriteArrayList<ILineHandler> delegates = new CopyOnWriteArrayList<ILineHandler>();
    	
    	public LineHandlerList() {
    		
    	}
    	
    	public void addHandler(ILineHandler handler) {
    		delegates.add(handler);
    	}
    	
    	public void removeHandler(ILineHandler handler) {
    		delegates.remove(handler);
    	}
    	
		@Override
		public void start(Process process) {
			for (ILineHandler delegate : delegates) {
				delegate.start(process);
			}
		}

		@Override
		public void newLine(String line) {
			for (ILineHandler delegate : delegates) {
				delegate.newLine(line);
			}
		}

		@Override
		public void stop(IOException e) {
			for (ILineHandler delegate : delegates) {
				delegate.stop(e);
			}
		}
    	
    }

    public static class XMLLineAdapter extends DefaultHandler implements ILineHandler {

    	StringBuffer buffer = new StringBuffer();

		@Override
		public void start(Process process) {
		}

		@Override
		public void newLine(String line) {
			buffer.append(line);
			buffer.append('\n');
		}

		@Override
		public final void stop(IOException ioe) {
			Exception ex = ioe;
			if (ioe == null) {
				try {
					SAXParserFactory factory = SAXParserFactory.newInstance();
					SAXParser parser = factory.newSAXParser();
					parser.parse(new ByteArrayInputStream(buffer.toString().getBytes()), this);
				} catch (Exception e) {
					ex = e;
				}
			}
			doStop(ex);
		}

		protected void doStop(Exception e) {

		}

    }

    private final Reader from;
    private final ILineHandler callback;

    public LineReader(Reader from, ILineHandler callback) {
        this.from = from;
        this.callback = callback;
    }

    public void start() {
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
	public void run() {
    	IOException ex = null;
        try {
            LineNumberReader reader = new LineNumberReader(from);
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (callback != null) {
                	callback.newLine(line);
                }
            }
        } catch (IOException e) {
        	ex = e;
            e.printStackTrace();
        } finally {
            if (callback != null) {
            	callback.stop(ex);
            }
        }
    }
}
