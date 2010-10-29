package com.mobilesorcery.sdk.capabilities.core;

import com.mobilesorcery.sdk.core.MoSyncProject;

/**
 * A change request is an operation that will change some property 
 * of an object -- one intended use case is to apply a "smart filter"
 * to a {@link MoSyncProject} that will make sure that projects using
 * say the Location API will have a filter that only allows devices
 * with Location support.
 * @author Mattias Bybro
 *
 */
public interface IChangeRequest {

	/**
	 * Applies this change request.
	 * TODO: Command pattern instead?
	 */
	public void apply();

	/**
	 * Returns the project for which this change request applies
	 * @return
	 */
	public MoSyncProject getProject();
	
}
