package com.mobilesorcery.sdk.core;


/**
 * <p>
 * Represents the profile info of an execution
 * </p>
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 *
 */
public interface IProfileInfo {

	/**
	 * Returns the number of times this file/line was hit
	 * during execution.
	 */
	public int getCount(String file, int line);
	
}
