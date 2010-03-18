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

import java.io.File;

import com.mobilesorcery.sdk.profiles.IProfile;

public interface IBuildResult {

	/**
	 * A flag indicating success/failure
	 * @return
	 */
	public abstract boolean success();
	
	/**
	 * Adds an error to this build result.
	 * @param errorMsg The error message describing the error
	 * that occurred.
	 */
    public abstract void addError(String errorMsg);

    /**
     * Returns a single file representing
     * the build result; this may be used
     * for sending via BT/OBEX, etc.
     * @return
     */
    public abstract File getBuildResult();

    /**
     * Sets the location of the build result.
     * @see getBuildResult
     * @param buildResult
     */
    public abstract void setBuildResult(File buildResult);

    // TODO: Move to some kind of 'build settings' class
    public abstract IProfile getProfile();


}