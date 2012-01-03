package com.mobilesorcery.sdk.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.osgi.internal.profile.DefaultProfileLogger;

import com.mobilesorcery.sdk.core.security.ICommonPermissions;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.IVendor;
import com.mobilesorcery.sdk.profiles.ProfileDBManager;
import com.mobilesorcery.sdk.profiles.filter.DeviceCapabilitiesFilter;

public abstract class ProfileManager {

	private boolean defaultProfileInited = false;

	private IProfile defaultProfile = null;

	public abstract void init();

	public abstract IVendor[] getVendors();

	public abstract IVendor getVendor(String vendorName);

	public abstract List<IProfile> getProfilesForRuntime(String runtime);

	/**
	 * <p>
	 * Returns the default target profile, which for a tool in a 'correct' state
	 * should return the default emulator profile.
	 * </p>
	 * <p>
	 * If no emulator profile exists, an arbitrary profile is returned.
	 * </p>
	 *
	 * @return
	 */
	public IProfile getDefaultTargetProfile() {
		if (!defaultProfileInited) {
			defaultProfileInited = true;
			init();
			IVendor[] vendors = getVendors();
			IProfile result = null;
			if (vendors.length > 0) {
				IVendor someVendor = vendors[0];
				IProfile[] profilesForVendor = someVendor.getProfiles();
				if (profilesForVendor.length > 0) {
					result = profilesForVendor[0];
				}
			}
			defaultProfile = result;
		}
		return defaultProfile;
	}

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

	/**
	 * Given a name of the format <code>vendor/profile</code>, will return an
	 * instance of <code>IProfile</code>
	 *
	 * @param fullName
	 * @return <code>null</code> if <code>fullName</code> is <code>null</code>,
	 *         or does not properly represent a profile
	 */
	public IProfile getProfile(String fullName) {
		if (fullName == null) {
			return null;
		}

		String[] tokens = fullName.split("/");
		if (tokens.length != 2) {
			return null;
		}

		String vendorName = tokens[0];
		String profileName = tokens[1];

		IVendor vendor = getVendor(vendorName);
		if (vendor == null) {
			return null;
		}

		return vendor.getProfile(profileName);
	}

	public String[] getAvailableCapabilities(boolean permissionsOnly) {
		return ICommonPermissions.ALL_PERMISSIONS;
	}

	public static IProfile[] filterProfiles(IProfile[] profiles,
			IDeviceFilter filter) {
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

	public static String toCanonicalRuntime(String runtime) {
		return Util.convertSlashes(runtime);
	}

}
