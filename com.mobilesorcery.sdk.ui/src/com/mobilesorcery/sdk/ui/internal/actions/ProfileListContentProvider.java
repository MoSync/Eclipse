package com.mobilesorcery.sdk.ui.internal.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.ProfileManager;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.IVendor;

public class ProfileListContentProvider implements IStructuredContentProvider {

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	@Override
	public Object[] getElements(Object inputElement) {
		ArrayList<Object> result = new ArrayList<Object>();
		IVendor[] vendors = null;
		HashSet<IProfile> profiles = null;
		if (inputElement instanceof MoSyncTool) {
			MoSyncTool tool = (MoSyncTool) inputElement;
			ProfileManager mgr = tool.getProfileManager(MoSyncTool.LEGACY_PROFILE_TYPE);
			vendors = mgr.getVendors();
			profiles = new HashSet<IProfile>(Arrays.asList(mgr.getProfiles()));
		} else if (inputElement instanceof MoSyncProject) {
			MoSyncProject project = (MoSyncProject) inputElement;
			vendors = (project).getFilteredVendors();
			profiles = new HashSet<IProfile>(Arrays.asList(project.getFilteredProfiles()));
		}

		for (int i = 0; vendors != null && i < vendors.length; i++) {
			IVendor vendor = vendors[i];
			result.add(vendor);
			IProfile[] profilesForVendor = vendor.getProfiles();
			for (int j = 0; j < profilesForVendor.length; j++) {
				if (profiles.contains(profilesForVendor[j])) {
					result.add(profilesForVendor[j]);
				}
			}
		}
		return result.toArray();
	}

}
