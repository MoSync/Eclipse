package com.mobilesorcery.sdk.capabilities.core;

import com.mobilesorcery.sdk.core.ICapabilities;
import com.mobilesorcery.sdk.core.MoSyncProject;

/**
 * Matches a set of capabilities 'requests' (ie the capabilites required
 * by the project to build properly), with a set of capabilities 'providers',
 * which are capabilities in the project, the typical example being 
 * the capabilities of the device such as "Location" or "Bluetooth".
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 *
 */
public interface ICapabilitiesMatcher {

	/**
	 * Performs a match of capabilities 'requests' and 'providers' and
	 * returns a <code>IChangeRequest</code>, which basically
	 * is an operations that will change some aspect of the project for which
	 * the requests and providers originated.
	 * @param requests
	 * @return <code>null</code> if the project already fulfills the required capabilites
	 */
	public IChangeRequest match(MoSyncProject project, ICapabilities requestedCapabilites);
	
}
