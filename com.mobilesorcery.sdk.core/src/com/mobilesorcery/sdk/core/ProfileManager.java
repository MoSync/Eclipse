package com.mobilesorcery.sdk.core;

import java.util.ArrayList;
import java.util.Arrays;

import com.mobilesorcery.sdk.core.security.ICommonPermissions;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.IVendor;

public abstract class ProfileManager {

	public abstract void init();

	public abstract IVendor[] getVendors();

	public abstract IVendor getVendor(String vendorName);

	public IVendor[] getVendors(IDeviceFilter filter) {
		IVendor[] allVendors = getVendors();
		ArrayList<IVendor> result = new ArrayList<IVendor>();

		for (int i = 0; i < allVendors.length; i++) {
			if (filter.accept(allVendors[i])) {
				result.add(allVendors[i]);
			}
		}

		return result.toArray(new IVendor[0]);
	}

	public IProfile[] getProfiles() {
		ArrayList<IProfile> profiles = new ArrayList<IProfile>();
		IVendor[] vendors = getVendors();
		for (int i = 0; i < vendors.length; i++) {
			profiles.addAll(Arrays.asList(vendors[i].getProfiles()));
		}

		return profiles.toArray(new IProfile[0]);
	}

	public IProfile[] getProfiles(IDeviceFilter filter) {
		IProfile[] profiles = getProfiles();
		return filterProfiles(profiles, filter);
	}

	public static IProfile[] filterProfiles(IProfile[] profiles, IDeviceFilter filter) {
		if (filter != null) {
			ArrayList<IProfile> filtered = new ArrayList<IProfile>();
			for (int i = 0; i < profiles.length; i++) {
				if (filter.accept(profiles[i])) {
					filtered.add(profiles[i]);
				}
			}
			return filtered.toArray(new IProfile[filtered.size()]);
		} else {
			return profiles;
		}
	}

	public IProfile[] getProfiles(String profileName) {
		ArrayList<IProfile> profiles = new ArrayList<IProfile>();
		IVendor[] vendors = getVendors();
		for (int i = 0; i < vendors.length; i++) {
			IProfile profile = vendors[i].getProfile(profileName);
			if (profile != null) {
				profiles.add(profile);
			}
		}

		return profiles.toArray(new IProfile[0]);
	}

	public String[] getAvailableCapabilities() {
		return ICommonPermissions.ALL_PERMISSIONS;
	}


}
