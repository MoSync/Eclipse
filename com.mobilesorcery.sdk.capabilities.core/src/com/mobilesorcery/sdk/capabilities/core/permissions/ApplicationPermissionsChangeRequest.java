package com.mobilesorcery.sdk.capabilities.core.permissions;

import java.text.MessageFormat;
import java.util.List;

import com.mobilesorcery.sdk.capabilities.core.AbstractChangeRequest;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.security.IApplicationPermissions;

public class ApplicationPermissionsChangeRequest extends AbstractChangeRequest {

	private List<String> missingPermissions;
	private IApplicationPermissions wc;

	public ApplicationPermissionsChangeRequest(MoSyncProject project, List<String> missingPermissions) {
		super(project);
		this.missingPermissions = missingPermissions;
	}

	/**
	 * Returns a working copy for <code>IChangeRequestEditor</code>s
	 * to use. (NOTE: Not yet implemented)
	 * @return
	 */
	public IApplicationPermissions getWorkingCopy() {
		if (wc == null) {
			wc = getProject().getPermissions().createWorkingCopy();
			wc.setRequestedPermissions(missingPermissions, true);
		}
		
		return wc;
	}
	
	public void apply() {
		IApplicationPermissions wc = getWorkingCopy();
		getProject().getPermissions().apply(wc);
	}

	public String toString() {
		String permissionList = Util.join(missingPermissions.toArray(new String[0]), ", ", 0, Math.min(3, missingPermissions.size() - 1));
		String ellipsis = missingPermissions.size() > 4 ? "..." : "";
		return MessageFormat.format("Add {0} permissions: {1}", missingPermissions.size(), permissionList + ellipsis);
	}

}
