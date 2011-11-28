package com.mobilesorcery.sdk.profiles.filter;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.internal.ProfileDBManager;
import com.mobilesorcery.sdk.profiles.IProfile;

public class DeviceCapabilitiesFilter extends AbstractDeviceFilter {

	private final static String FACTORY_ID = "com.mobilesorcery.sdk.capabilities.devices.elementfactory";

	private final String[] requiredCapabilities;
	private final String[] optionalCapabilities;

	private HashSet<IProfile> profiles;

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
			Set<IProfile> matches = mgr.match("*", requiredCapabilities, optionalCapabilities);
			profiles.addAll(matches);
		}
	}

	@Override
	public String toString() {
		return MessageFormat.format("Required: {0}, Optional: {1}", Util.join(requiredCapabilities, ","), Util.join(optionalCapabilities, ","));
	}
}
