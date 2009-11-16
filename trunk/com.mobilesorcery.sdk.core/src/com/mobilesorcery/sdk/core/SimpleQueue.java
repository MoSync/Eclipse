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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * A simple queue implementation; 0-1 threads per queue
 * @author Mattias Bybro
 *
 */
public class SimpleQueue {

	private ThreadPoolExecutor internalQueue = null;
	private boolean shutdown = false;
	
	public SimpleQueue(boolean keepalive) {
		internalQueue = new ThreadPoolExecutor(0, 1, keepalive ? Long.MAX_VALUE : 2, keepalive ? TimeUnit.NANOSECONDS : TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	}
	
	public void execute(Runnable r) {
		if (shutdown) {
			CoreMoSyncPlugin.trace("Shutdown, runnable not executed");
			return;
		}
		internalQueue.execute(r);
	}
	
	/**
	 * Shuts down this queue - all remaining tasks
	 * are executed. This method returns when all
	 * tasks have been executed or the specified
	 * timeout has expired.
	 * 
	 * @param timeout Timeout in ms, <code>0</code> will cause this method
	 * to return immediately.
	 */
	public synchronized void awaitShutdown(int timeout) {
		if (shutdown) {
			return;
		}
		shutdown = true;
		
		internalQueue.shutdown();

		if (timeout > 0) {
			try {
				internalQueue.awaitTermination(timeout, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				// Just return.
			}
		}
	}
}
