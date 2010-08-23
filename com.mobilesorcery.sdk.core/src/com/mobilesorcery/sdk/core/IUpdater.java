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
 * <p>An interface for updating the entire MoSync SDK, including
 * this eclipse-based IDE.</p>
 * <p>This interface is used by the com.mobilesorcery.sdk.update
 * extension point</p>
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 *
 */
public interface IUpdater {

    /**
     * Performs an update request to the update server
     * @param isStartedByUser whether this request
     * was explicitly invoked by the user
     */
	public void update(boolean isStartedByUser);
	
	/**
	 * Launches a user interface allowing the user
	 * to register with MoSync
     * @param isStartedByUser whether this request
     * was explicitly invoked by the user
	 */
	public void register(boolean isStartedByUser);
	
	/**
	 * This method is called by the framework to
	 * indicate that the <code>IUpdater</code> will
	 * no longer be used. Clients may implement
	 * clean-up functionality here.
	 */
	public void dispose();
}
