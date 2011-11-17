package com.mobilesorcery.sdk.capabilities.devices.internal;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.mobilesorcery.sdk.core.Capabilities;
import com.mobilesorcery.sdk.core.CapabilityState;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.ICapabilities;
import com.mobilesorcery.sdk.core.MergeCapabilities;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Pair;
import com.mobilesorcery.sdk.core.SectionedPropertiesFile;
import com.mobilesorcery.sdk.core.SectionedPropertiesFile.Section;
import com.mobilesorcery.sdk.core.SectionedPropertiesFile.Section.Entry;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.Profile;

public class DeviceCapabilitiesParser {

	class CapabilitiesMap {

		List<Pair<Pattern, Capabilities>> runtimes = new ArrayList<Pair<Pattern,Capabilities>>();
		List<Pair<Pattern, Capabilities>> profiles = new ArrayList<Pair<Pattern,Capabilities>>();

		public void addRuntimeCapability(Pattern pattern, Capabilities capabilites) {
			if (pattern != null && capabilites != null) {
				runtimes.add(new Pair<Pattern, Capabilities>(pattern, capabilites));
			}
		}

		public void addProfileCapability(Pattern pattern, Capabilities capabilites) {
			if (pattern != null && capabilites != null) {
				profiles.add(new Pair<Pattern, Capabilities>(pattern, capabilites));
			}
		}

		public ICapabilities getCapabilities(IProfile requestProfile) {
			String profileName = MoSyncTool.toString(requestProfile);
			String runtimeName = Profile.getAbbreviatedPlatform(requestProfile);

			ArrayList<Capabilities> result = new ArrayList<Capabilities>();

			for (Pair<Pattern, Capabilities> runtime : runtimes) {
				if (runtime.first.matcher(runtimeName).matches()) {
					result.add(runtime.second);
				}
			}

			for (Pair<Pattern, Capabilities> profile : profiles) {
				if (profile.first.matcher(profileName).matches()) {
					result.add(profile.second);
				}
			}

			// Correct inheritance
			Collections.reverse(result);
			if (result.isEmpty()) {
				return null;
			} else {
				return new MergeCapabilities(result.toArray(new ICapabilities[0]));
			}
		}

	}

	private static final String CAPABILITIES_FILENAME = "runtime-capabilities.map";

	private final HashMap<IProfile, ICapabilities> capabilities = new HashMap<IProfile, ICapabilities>();
	private final HashMap<IProfile, CapabilitiesMap> profileCapabilities = new HashMap<IProfile, CapabilitiesMap>();
	private final HashMap<String, CapabilitiesMap> runtimeCapabilities = new HashMap<String, CapabilitiesMap>();
	private CapabilitiesMap defaultCapabilities;

	public DeviceCapabilitiesParser() {

	}

	private CapabilitiesMap parse(File file) throws IOException {
		FileReader input = null;
		try {
			if (!file.exists()) {
				return null;
			}
			input = new FileReader(file);
			return parse(input);
		} finally {
			Util.safeClose(input);
		}
	}

	private CapabilitiesMap parse(Reader input) throws IOException {
		SectionedPropertiesFile capabilitesMap = SectionedPropertiesFile.parse(input);
		CapabilitiesMap result = new CapabilitiesMap();

		for (Section section : capabilitesMap.getSections()) {
			Capabilities capabilities = null;
			List<Entry> entries = section.getEntries();
			for (Entry entry : entries) {
				String capability = entry.getKey();
				String capabilityStateStr = entry.getValue();
				if (capabilities == null) {
					capabilities = new Capabilities();
				}

				CapabilityState capabilityState = CapabilityState.SUPPORTED;

				try {
					capabilityState = CapabilityState.valueOf(capabilityStateStr);
				} catch (Exception e) {
					// Fallback to default: SUPPORTED
				}

				capabilities.setCapability(capability, capabilityState);
			}
			addCapabilites(result, section.getName(), capabilities);
		}

		return result;
	}

	private void addCapabilites(CapabilitiesMap capabilitiesMap, String matchingCriteria, Capabilities capabilities) {
		if (matchingCriteria == null) {
			return;
		}
		try {
			if (matchingCriteria.startsWith("profile:")) {
				capabilitiesMap.addProfileCapability(Pattern.compile(matchingCriteria.substring("profile:".length())), capabilities);
			} else if (matchingCriteria.startsWith("runtime:")) {
				capabilitiesMap.addRuntimeCapability(Pattern.compile(matchingCriteria.substring("runtime:".length())), capabilities);
			} else {
				capabilitiesMap.addRuntimeCapability(Pattern.compile(matchingCriteria), capabilities);
			}
		} catch (PatternSyntaxException e) {
			// Just log & ignore
			CoreMoSyncPlugin.getDefault().log(e);
		}
	}

	public ICapabilities getCapabilitiesForProfile(IProfile profile) {
		if (capabilities.get(profile) == null) {
			try {
				capabilities.put(profile, createCapabilities(profile));
			} catch (IOException e) {
				CoreMoSyncPlugin.getDefault().log(e);
			}
		}

		return capabilities.get(profile);
	}


	private ICapabilities createCapabilities(IProfile profile) throws IOException {
		CapabilitiesMap defaultMap = getDefaultCapabilitiesMap();
		CapabilitiesMap runtimeMap = getCapabilitiesMapForRuntime(profile.getPlatform());
		CapabilitiesMap profileMap = getCapabilitiesMapForProfile(profile);
		return assembleCapabilities(profile, defaultMap, runtimeMap, profileMap);
	}

	private ICapabilities assembleCapabilities(IProfile profile, CapabilitiesMap... maps) {
		ArrayList<ICapabilities> result = new ArrayList<ICapabilities>();
		for (CapabilitiesMap map : maps) {
			ICapabilities partialCapabilities = map == null ? null : map.getCapabilities(profile);
			if (partialCapabilities != null) {
				result.add(partialCapabilities);
			}
		}

		return new MergeCapabilities(result.toArray(new ICapabilities[0]));
	}

	private CapabilitiesMap getCapabilitiesMapForProfile(IProfile profile) throws IOException {
		if (profileCapabilities.get(profile) == null) {
			profileCapabilities.put(profile, createCapabilitiesMap(profile));
		}
		return profileCapabilities.get(profile);
	}

	private CapabilitiesMap getCapabilitiesMapForRuntime(String platform) throws IOException {
		if (runtimeCapabilities.get(platform) == null) {
			runtimeCapabilities.put(platform, createCapabilitiesMap(platform));
		}
		return runtimeCapabilities.get(platform);
	}

	private CapabilitiesMap getDefaultCapabilitiesMap() throws IOException {
		if (defaultCapabilities == null) {
			defaultCapabilities = createDefaultCapabilitiesMap();
		}
		return defaultCapabilities;
	}

	private CapabilitiesMap createCapabilitiesMap(IProfile profile) throws IOException {
		File profileFile = MoSyncTool.getDefault().getProfilePath(profile).append(CAPABILITIES_FILENAME).toFile();
		return parse(profileFile);
	}

	private CapabilitiesMap createCapabilitiesMap(String platform) throws IOException {
		File runtimeFile = MoSyncTool.getDefault().getRuntimePath(platform).append(CAPABILITIES_FILENAME).toFile();
		CapabilitiesMap runtimeCapabilites = parse(runtimeFile);
		return runtimeCapabilites;
	}

	private CapabilitiesMap createDefaultCapabilitiesMap() throws IOException {
		File defaultFile = MoSyncTool.getDefault().getProfilesPath().append(CAPABILITIES_FILENAME).toFile();
		return parse(defaultFile);
	}
}
