package com.mobilesorcery.sdk.core;

import java.io.File;

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

}