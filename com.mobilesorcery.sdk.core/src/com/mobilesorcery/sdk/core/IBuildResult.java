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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;

import com.mobilesorcery.sdk.internal.dependencies.DependencyManager.Delta;

public interface IBuildResult {

	/**
	 * The main build result file key
	 */
	public static final String MAIN = "main";

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
     * Returns the list of error messages.
     * @return
     */
    public abstract List<String> getErrors();

    /**
     * Returns the files representing
     * the build result; this may be used
     * for sending via BT/OBEX, etc.
     * @return A map with keys indicating the
     * type of the build artifact (this key may
     * be null). The special key {@link MAIN} is used
     * to indicate files that can be used for
     * uploading, etc.
     */
    public abstract Map<String, List<File>> getBuildResult();

    /**
     * Sets the the build result.
     * @see getBuildResult
     * @param buildResult
     */
    public abstract void setBuildResult(Map<String, List<File>> buildResult);

    /**
     * Sets the location of the build result, for a
     * specific key.
     * @see getBuildResult
     * @param buildResult
     */
    public abstract void setBuildResult(String key, File... buildResult);

    /**
     * Returns the variant built.
     * @return
     */
    public abstract IBuildVariant getVariant();

    /**
     * Returns the timestamp of this build result (set before
     * actually building)
     * @return The timestamp as UTC
     */
    public abstract long getTimestamp();

    /**
     * Sets the timestamp of this build result
     * @param utc
     */
    public abstract void setTimestamp(long utc);

    /**
     * Returns the set of resources that needed to be rebuilt
     * during this build.
     * @return
     */
	public abstract Delta<IResource> getDependencyDelta();


	/**
	 * Gets an 'intermediate build step'
	 * @param buildStepId
	 * @param file
	 */
	public abstract File getIntermediateBuildResult(String buildStepId);

	/**
	 * Sets an 'intermediate build step'
	 * @param buildStepId
	 * @param file
	 */
	public abstract void setIntermediateBuildResult(String buildStepId, File file);

}