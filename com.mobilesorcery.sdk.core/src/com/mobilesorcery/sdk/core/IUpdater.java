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

	public void update(boolean isStartedByUser);
	
}
