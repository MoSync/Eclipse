package com.mobilesorcery.sdk.profiles.filter;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.ProfileManager;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.internal.ProfileDBManager;
import com.mobilesorcery.sdk.internal.ProfileDBManager.ProfileDBResult;
import com.mobilesorcery.sdk.profiles.ICompositeDeviceFilter;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.filter.elementfactories.DeviceCapabilitiesFilterFactory;

public class DeviceCapabilitiesFilter extends AbstractDeviceFilter {

	private final static String FACTORY_ID =  DeviceCapabilitiesFilterFactory.ID;

	private final String[] requiredCapabilities;
	private final String[] optionalCapabilities;

	private HashSet<IProfile> profiles;

	private Map<String, String> profileMappings;

	private HashMap<String, List<IProfile>> profilesForRuntime;

	public DeviceCapabilitiesFilter(String[] requiredCapabilities, String[] optionalCapabilities) {
		this.requiredCapabilities = requiredCapabilities;
		this.optionalCapabilities = optionalCapabilities;
	}

	public Set<String> getRequiredCapabilities() {
		return new HashSet<String>(Arrays.asList(requiredCapabilities));
	}

	public Set<String> getOptionalCapabilities() {
		return new HashSet<String>(Arrays.asList(optionalCapabilities));
	}

	@Override
	public String getFactoryId() {
		return FACTORY_ID;
	}

	@Override
	public void saveState(IMemento memento) {
		IMemento capabilities = memento.createChild("capabilities");
		capabilities.putString("required", PropertyUtil.fromStrings(requiredCapabilities));
		capabilities.putString("optional", PropertyUtil.fromStrings(optionalCapabilities));
	}

	@Override
	public boolean acceptProfile(IProfile profile) {
		initProfiles();
		return profiles.contains(profile);
	}

	private void initProfiles() {
		if (profiles == null) {
			profiles = new HashSet<IProfile>();
			ProfileDBManager mgr = (ProfileDBManager) MoSyncTool.getDefault()
					.getProfileManager(MoSyncTool.DEFAULT_PROFILE_TYPE);
			ProfileDBResult matchResult = mgr.match("*", requiredCapabilities, optionalCapabilities);
			profiles.addAll(matchResult.profiles);
			profileMappings = matchResult.profileMappings;
			profilesForRuntime = matchResult.profilesForRuntime;
		}
	}

	public static DeviceCapabilitiesFilter extractFilterFromProject(MoSyncProject project) {
		ICompositeDeviceFilter compositeFilter = project.getDeviceFilter();
		IDeviceFilter[] filters = compositeFilter.getFilters();
		for (int i = 0; i < filters.length; i++) {
			IDeviceFilter filter = filters[i];
			if (filter instanceof DeviceCapabilitiesFilter) {
				DeviceCapabilitiesFilter result = (DeviceCapabilitiesFilter) filter;
				result.initProfiles();
				return result;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return MessageFormat.format("Required: {0}, Optional: {1}", Util.join(requiredCapabilities, ","), Util.join(optionalCapabilities, ","));
	}

	public IProfile mapProfile(IProfile profile) {
		if (profile == null) {
			return null;
		}
		if (acceptProfile(profile)) {
			return profile;
		}
		String mapped = profileMappings.get(MoSyncTool.toString(profile));
		return MoSyncTool.getDefault().getProfile(mapped);
	}

	/**
	 * Given a profile of the {@code MoSyncTool#LEGACY_PROFILE_TYPE} type,
	 * returns the closest matching profile of
	 * {@code MoSyncTool#DEFAULT_PROFILE_TYPE} for a project
	 * @param profile
	 * @return {@code null} If the profile would be filtered out by the project
	 */
	public static IProfile matchLegacyProfile(MoSyncProject project, IProfile profile) {
		String runtime = ProfileManager.toCanonicalRuntime(profile.getRuntime());
		DeviceCapabilitiesFilter dcf = extractFilterFromProject(project);
		List<IProfile> profiles = dcf.profilesForRuntime.get(runtime);
		IProfile firstMatch = profiles == null ? null : profiles.get(0);
		IProfile mappedProfile = dcf.mapProfile(firstMatch);
		return mappedProfile;
	}
}
