package com.mobilesorcery.sdk.capabilities.core.permissions;

import java.util.ArrayList;
import java.util.List;

import com.mobilesorcery.sdk.capabilities.core.ICapabilitiesMatcher;
import com.mobilesorcery.sdk.capabilities.core.IChangeRequest;
import com.mobilesorcery.sdk.core.ICapabilities;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.security.IApplicationPermissions;

/**
 * A {@link ICapabilitiesMatcher} for application permissions. The
 * {@link IChangeRequest} returned will add (but never remove) permissions to 
 * make it compatible with set of requested capabilities.
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 *
 */
public class ApplicationPermissionsCapabilitesMatcher implements ICapabilitiesMatcher {

	public IChangeRequest match(MoSyncProject project, ICapabilities requestedCapabilites) {
		IApplicationPermissions permissions = project.getPermissions();
		// We'll just use the convention that the permission id is also the API capability id
		List<String> availablePermissions = permissions.getAvailablePermissions(null);
		List<String> missingPermissions = new ArrayList<String>();
		for (String permission : availablePermissions) {
			if (requestedCapabilites.hasCapability(permission) && !permissions.isPermissionRequested(permission)) {
				missingPermissions.add(permission);
			}
		}
		
		return missingPermissions.isEmpty() ? null : new ApplicationPermissionsChangeRequest(project, missingPermissions);
	}

}
