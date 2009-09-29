package com.mobilesorcery.sdk.profiles;

/**
 * <p>A provider interface for <code>ITarget</code>s.
 * (The purpose of this class is just to keep an implementation-less
 * interface for UI purposes - see ProfilesView in the ui plugin)</p>
 * @author Mattias Bybro, mattias.bybro@purplescout.com/mattias@bybro.com
 *
 */
public interface ITargetProfileProvider {

	/**
	 * <p>Returns a target profile.</p>	
	 * @return
	 */
	public IProfile getTargetProfile();
	
}
