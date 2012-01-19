package com.mobilesorcery.sdk.internal.convert;

import org.eclipse.core.runtime.CoreException;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Version;
import com.mobilesorcery.sdk.core.MoSyncProject.IConverter;
import com.mobilesorcery.sdk.core.security.IApplicationPermissions;
import com.mobilesorcery.sdk.core.security.ICommonPermissions;

public class MoSyncProjectConverter1_4 implements IConverter {

	public final static Version VERSION = new Version("1.4");

	private static IConverter instance = new MoSyncProjectConverter1_4();

	public static MoSyncProject.IConverter getInstance() {
		return instance;
	}

	@Override
	public void convert(MoSyncProject project) throws CoreException {
		if (VERSION.isNewer(project.getFormatVersion())) {
			IApplicationPermissions permissions = project.getPermissions();
			IApplicationPermissions wc = permissions.createWorkingCopy();
			replacePermission(wc, ICommonPermissions.AUTOSTART_1_2, ICommonPermissions.AUTOSTART);
			replacePermission(wc, ICommonPermissions.VIBRATE_1_2, ICommonPermissions.VIBRATE);
			permissions.apply(wc);
			project.setFormatVersion(VERSION);
		}
	}

	private void replacePermission(IApplicationPermissions wc,
			String oldPermission, String newPermission) {
		if (wc.isPermissionRequested(oldPermission)) {
			wc.setRequestedPermission(oldPermission, false);
			wc.setRequestedPermission(newPermission, true);
		}
	}
}
