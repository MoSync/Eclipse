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
/**
 * 
 */
package com.mobilesorcery.sdk.internal.launch;

import java.io.IOException;
import java.io.PipedOutputStream;
import java.util.concurrent.CountDownLatch;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.ISLDInfo;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.internal.EmulatorOutputParser;
import com.mobilesorcery.sdk.internal.EmulatorOutputParser.ParseEvent;

public class EmulatorParseEventHandler implements EmulatorOutputParser.IParseEventHandler {

    /**
     * A listener interface for close listeners
     * @author Mattias
     *
     */
    /*interface ICloseListener {
        public void closed();
    }*/
    
    private MoSyncProject project = null;
    private ISLDInfo sldInfo = null;

    private PipedOutputStream messageStream;

    private CountDownLatch sldLatch = null;

    private String exitMessage;
    
    private int emulatorId = -1;
    
	private IBuildConfiguration buildConfiguration;

    public EmulatorParseEventHandler(MoSyncProject project, IBuildConfiguration buildConfiguration) {
        this.project = project;
        this.buildConfiguration = buildConfiguration;
        startSLDParsing();
    }

    private void startSLDParsing() {
    	sldLatch = new CountDownLatch(1);
    	Runnable sldRunnable = new Runnable() {
			public void run() {
				try {
					ISLDInfo oldSLDInfo = sldInfo;
					sldInfo = project.getSLD(buildConfiguration).parseSLD();
					if (CoreMoSyncPlugin.getDefault().isDebugging()) {
						if (oldSLDInfo == sldInfo) {
							CoreMoSyncPlugin.trace("Using cached SLD for " + project.getName());
						} else {
							CoreMoSyncPlugin.trace("Done parsing sld for " + project.getName());
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					sldLatch.countDown();
				}
			}
    	};
    	
    	Thread sldThread = new Thread(sldRunnable);    	
    	sldThread.setName("Parsing SLD for project " + project.getName());
    	sldThread.start();
	}

	public void setEmulatorId(int id) {
        this.emulatorId = id;
    }
    
    public void handleEvent(ParseEvent event) {
        int[] stack = event.stack;

        try {
            if (CoreMoSyncPlugin.getDefault().isDebugging()) {
            	CoreMoSyncPlugin.trace(event);
            }

            switch (event.type) {
            case EmulatorOutputParser.REPORT_STRING:
            case EmulatorOutputParser.REPORT_EXIT_STRING:
                messageStream.write(emulatorId(event.message).getBytes());
                messageStream.write('\n');
                break;
            case EmulatorOutputParser.REPORT_IP:
                stack = new int[] { event.ip };
                // fall thru
            case EmulatorOutputParser.REPORT_CALL_STACK:
                ISLDInfo sld = getSLD();
                for (int i = 0; stack != null && i < stack.length; i++) {
                    String filename = sld == null ? null : sld.getFileName(stack[i]);
                    int line = sld == null ? -1 : sld.getLine(stack[i]);
                    String sldMsg = (filename == null ? "Unknown file" : filename) + (line > 0 ? (":" + line) : "");
                    // console.addMessage("0x" + Integer.toHexString(stack[i]) +
                    // ": " + sldMsg);
                    messageStream.write(emulatorId("IP:0x" + Integer.toHexString(stack[i]) + ": " + sldMsg).getBytes());
                    messageStream.write('\n');
                }
                break;
            case EmulatorOutputParser.REPORT_LOAD_PROGRAM:
            case EmulatorOutputParser.REPORT_RELOAD:
                sld = null;
                break;
            }
        } catch (Exception e) {
            // Ignore.
        	e.printStackTrace();
        }
    }

    private String emulatorId(String msg) {
        return emulatorId > 0 ? "[" + emulatorId + "] " + msg : msg;
    }

    private ISLDInfo getSLD() throws IOException {
    	try {
    		if (sldLatch.getCount() > 0) {
    			messageStream.write("Reading line number information - may take a few moments\n".getBytes());
    		}
    		
			sldLatch.await();
			return sldInfo;
		} catch (InterruptedException e) {
			throw new IOException(e.getMessage());
		}        
    }

    /**
     * @param messageStream
     */
    public void setMessageOutputStream(PipedOutputStream messageStream) {
        this.messageStream = messageStream;
    }

    public void setExitMessage(String exitMessage) {
        this.exitMessage = exitMessage;
    }
    
    public String getExitMessage() {
    	return exitMessage;
    }

}