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

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A manager for listening to emulator process events
 * @see IEmulatorProcessListener
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 *
 */
public class EmulatorProcessManager {

	private CopyOnWriteArrayList<IEmulatorProcessListener> listeners = new CopyOnWriteArrayList<IEmulatorProcessListener>();
	private Map<IEmulatorProcessListener, Integer> specifiedListeners = Collections.synchronizedMap(new IdentityHashMap<IEmulatorProcessListener, Integer>());
	
	EmulatorProcessManager() {
		// Package private.
	}
	
	/**
	 * Adds a listener to this manager - whenever an emulator is started
	 * through an <code>ILaunchConfiguration</code>, all listeners added
	 * by this method will be notified
	 * @param listener
	 */
	public void addEmulatorProcessListener(IEmulatorProcessListener listener) {
		listeners.add(listener);	
	}
	
	/**
	 * Adds a listner to this manager that only gets notified upon events
	 * originating from an emulator process with a specified emulator id.
	 * @param emulatorId
	 * @param listener
	 */
	public void addEmulatorProcessListener(int emulatorId, IEmulatorProcessListener listener) {
		addEmulatorProcessListener(listener);
		specifiedListeners.put(listener, emulatorId);
	}

	/**
	 * Removes a listener from this manager
	 * @param listener
	 */
	public void removeEmulatorProcessListener(IEmulatorProcessListener listener) {
		listeners.remove(listener);		
		specifiedListeners.remove(listener);
	}
	
	public void processStarted(int id) {		
		for (IEmulatorProcessListener listener : listeners) {
			try {
				if (shouldNotify(listener, id)) {
					listener.processStarted(id);
				}
			} catch (Throwable t) {
				// Ignore.
			}
		}
	}

	public void processStopped(int id) {		
		for (IEmulatorProcessListener listener : listeners) {
			try {
				if (shouldNotify(listener, id)) {
					listener.processStopped(id);
				}
			} catch (Throwable t) {
				// Ignore.
			}
		}
	}

	public void dataStreamed(int id, byte[] data, int offset, int length) {		
		for (IEmulatorProcessListener listener : listeners) {
			try {
				if (shouldNotify(listener, id)) {
					listener.dataStreamed(id, data, offset, length);
				}
			} catch (Throwable t) {
				// Ignore.
			}
		}
	}

	private boolean shouldNotify(IEmulatorProcessListener listener, int id) {
		Integer specified = specifiedListeners.get(listener);
		return specified == null || specified.intValue() == id;
	}
	
}
