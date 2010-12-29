package com.mobilesorcery.sdk.builder.java.capabilities;

import java.util.ArrayList;

import com.mobilesorcery.sdk.capabilities.core.AddDeviceFilterChangeRequest;
import com.mobilesorcery.sdk.capabilities.core.ICapabilities;
import com.mobilesorcery.sdk.capabilities.core.ICapabilitiesMatcher;
import com.mobilesorcery.sdk.capabilities.core.IChangeRequest;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.security.ICommonPermissions;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.filter.ProfileFilter;


/**
 * <p>
 * A class that helps the user to filter out those capabilites
 * that typically requires an operator certificate to work
 * (in practice you don't want to publish to those devices)
 * </p>
 * <p>The strategy used is to create a <code>IChangeRequest</code> 
 * for all requested capabilities that matches:
 * <ul>
 * <li>A default MIDP 3rd party protection domain of "No" or "Oneshot"
 * <br>(as per defined in the MIDP 2.0.1 addendum "The Recommended Security Policy for GSM/UMTS Compliant Devices")
 * <li>Exception: SMS, Internet access
 * </ul>
 * where the recom
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 *
 */
public class JavaMEOpCertCapabilitiesMatcher implements ICapabilitiesMatcher {

	public IChangeRequest match(MoSyncProject project, ICapabilities requestedCapabilities) {
		IProfile[] profiles = project.getFilteredProfiles();
		JavaMEOpCertFilter filter = new JavaMEOpCertFilter(requestedCapabilities);
		for (int i = 0; i < profiles.length; i++) {
			IProfile profile = profiles[i];
			if (!filter.accept(profile)) {
				AddDeviceFilterChangeRequest changeRequest = new AddDeviceFilterChangeRequest(project, filter);
				changeRequest.setMessage("Remove {0} JavaME profiles that might require operator certificates");
				changeRequest.setPreferredApplyState(false);
				return changeRequest;
			}
		}
		
		return null;
	}
	
	

}
