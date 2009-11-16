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
package com.mobilesorcery.sdk.testing.emulator;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;


import org.eclipse.debug.core.ILaunchConfiguration;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IEmulatorProcessListener;
import com.mobilesorcery.sdk.testing.IRelaunchableTestSession;
import com.mobilesorcery.sdk.testing.TestSession;
import com.mobilesorcery.sdk.testing.TestSessionEvent;

public class EmulatorTestSession extends TestSession implements IEmulatorProcessListener, IRelaunchableTestSession {

	static final byte[] TEST_MARKUP_PREFIX = "__TEST_MARKUP__".getBytes();
	static final int TEST_MARKUP_PREFIX_LEN = TEST_MARKUP_PREFIX.length;
		
	public final static String FILE_KEY = XMLTestReportParser.FILE_ATTR;			
	public final static String LINE_KEY = XMLTestReportParser.LINE_ATTR;

	private boolean parserStarted;
	private PipedOutputStream buffer;
	private ILaunchConfiguration launchConfig;
	private int emulatorId;
	
	public EmulatorTestSession(String name, ILaunchConfiguration launchConfig, int emulatorId) {
		super(name);
		this.launchConfig = launchConfig;
		this.emulatorId = emulatorId;
	}

	public void start() {
		checkStartable();
		CoreMoSyncPlugin.getDefault().getEmulatorProcessManager().addEmulatorProcessListener(emulatorId, this);
		// Expects it to be started externally...
		notifyListeners(new TestSessionEvent(TestSessionEvent.SESSION_STARTED, this, this));
	}
	
	public void finish() {
		CoreMoSyncPlugin.getDefault().getEmulatorProcessManager().removeEmulatorProcessListener(this);
		notifyListeners(new TestSessionEvent(TestSessionEvent.SESSION_STARTED, this, this));
	}
	
	public void dataStreamed(int id, byte[] data, int offset, int length) {
		try {
			if (length < TEST_MARKUP_PREFIX_LEN) {
				return;
			}
			
			for (int i = 0; i < TEST_MARKUP_PREFIX_LEN; i++) {
				if (TEST_MARKUP_PREFIX[i] != data[offset + i]) {
					return;
				}
			}
			
			feedIntoParser(data, offset + TEST_MARKUP_PREFIX_LEN, length - TEST_MARKUP_PREFIX_LEN);
		} catch (IOException e) {
			// We don't report this to the UI, since it will for sure be reported by the other thread;
			// if we would report it, a potentially large number of spurious error messages will show up.
			e.printStackTrace();
		}
	}

	private void feedIntoParser(byte[] data, int offset, int length) throws IOException {
		if (!parserStarted) {
			parserStarted = true;
			XMLTestReportParser parser = new XMLTestReportParser(this, this);
			PipedInputStream input = new PipedInputStream();
			buffer = new PipedOutputStream();
			buffer.connect(input);
			parser.parse(input);
		}
		
		buffer.write(data, offset, length);
	}

	public void processStarted(int id) {
	}

	public void processStopped(int id) {
		try {
			buffer.close();
		} catch (IOException e) {
			reportSessionError(e);
		}
	}

	void reportSessionError(Exception e) {
		getTestResult().addFailure(this, e);	
	}

	public ILaunchConfiguration getLaunchConfiguration() {
		return launchConfig;
	}

}