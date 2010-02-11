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
package com.mobilesorcery.sdk.smoketests;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.cdt.debug.core.model.ICThread;
import org.eclipse.cdt.debug.mi.core.GDBProcess;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IEmulatorProcessListener;

public class MockEmulatorProcessListener implements IEmulatorProcessListener, IDebugEventSetListener {

	private StringBuffer data = new StringBuffer();
	private int id;
	private CountDownLatch latch = new CountDownLatch(1);
	private CountDownLatch breakpointLatch = new CountDownLatch(1);

	public void dataStreamed(int id, byte[] data, int offset, int length) {
		this.data.append(new String(data, offset, length));
	}
	
	public String getStreamedData() {
		return data.toString();
	}

	public void processStarted(int id) {
		this.id = id;
	}

	public void processStopped(int id) {
		latch.countDown();
	}

	public void awaitStopped(long timeout, TimeUnit unit) throws InterruptedException {
		if (!latch.await(timeout, unit)) {
			throw new IllegalStateException("Timeout awaiting process stopped");
		}
	}

	public void handleDebugEvents(DebugEvent[] events) {
		for (int i = 0; i < events.length; i++) {
			handleDebugEvent(events[i]);
		}
	}

	private void handleDebugEvent(DebugEvent debugEvent) {
		if (debugEvent.getKind() == DebugEvent.BREAKPOINT) {
			Object source = debugEvent.getSource();
			if (source instanceof ICThread || source instanceof GDBProcess) {
				breakpointLatch.countDown();
			}
		}		
	}

	public void awaitBreakpointHit(long timeout, TimeUnit unit) throws InterruptedException {
		if (!breakpointLatch.await(timeout, unit)) {
			throw new IllegalStateException("Breakpoint never hit");
		}
	}

	public void connect() {
		DebugPlugin.getDefault().addDebugEventListener(this);
		CoreMoSyncPlugin.getDefault().getEmulatorProcessManager().addEmulatorProcessListener(this);
	}
	
	public void disconnect() {
		CoreMoSyncPlugin.getDefault().getEmulatorProcessManager().removeEmulatorProcessListener(this);
		DebugPlugin.getDefault().removeDebugEventListener(this);
	}


}
