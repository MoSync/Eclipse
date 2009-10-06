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

/**
 * <p>A listener interface to handle emulator process events. Events include
 * <ul>
 * <li>Starting an emulator process</li>
 * <li>Stopping an emulator process</li>
 * <li>Data passed to the emulator's stdout/stderr</li>
 * </ul>
 * </p>
 * <p>The intended usage for this class is automated testing.</p>
 * @author Mattias Bybro, mattias.bybro@purplescout.com/mattias@bybro.com
 *
 */
public interface IEmulatorProcessListener {

	/**
	 * Called when an emulator process is started
	 * @param id The id of the emulator process (this is NOT the OS pid)
	 */
	public void processStarted(int id);
	
	/**
	 * Called when an emulator process is stopped
	 * @param id The id of the emulator process (this is NOT the OS pid)
	 */
	public void processStopped(int id);
	
	/**
	 * Data being passed via the emulator stdout/stderr
	 * @param id
	 * @param data
	 * @param length 
	 * @param offset 
	 */
	public void dataStreamed(int id, byte[] data, int offset, int length);
}
