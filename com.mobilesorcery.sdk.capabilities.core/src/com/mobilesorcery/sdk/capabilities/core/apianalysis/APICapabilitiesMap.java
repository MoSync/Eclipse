package com.mobilesorcery.sdk.capabilities.core.apianalysis;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mobilesorcery.sdk.capabilities.core.Capabilities;
import com.mobilesorcery.sdk.capabilities.core.ICapabilities;
import com.mobilesorcery.sdk.capabilities.core.Capabilities.MergeCapabilities;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.Pair;
import com.mobilesorcery.sdk.core.SectionedPropertiesFile;
import com.mobilesorcery.sdk.core.SectionedPropertiesFile.Section;
import com.mobilesorcery.sdk.core.SectionedPropertiesFile.Section.Entry;

public class APICapabilitiesMap {

	private static APICapabilitiesMap instance = new APICapabilitiesMap();

	private HashMap<Pair<URI, String>, Capabilities> capabilitiesMap = new HashMap<Pair<URI, String>, Capabilities>();
	private HashMap<URI, Capabilities> fileCapabilitiesMap = new HashMap<URI, Capabilities>();

	private Set<File> initedDirectories = new HashSet<File>();

	public static APICapabilitiesMap getInstance() {
		return instance;
	}

	private APICapabilitiesMap() {

	}

	private void initCapabilitesMapForDirectory(File directory) {
		// Another option is to add source annotations. (Or use libs instead,
		// but that would
		// be another solution altogether.
		File capabilitiesFile = new File(directory, "capabilities.map");
		try {
			if (!initedDirectories.contains(capabilitiesFile)) {
				initedDirectories.add(capabilitiesFile);
				if (capabilitiesFile.exists()) {
					SectionedPropertiesFile capabilitesProperties = SectionedPropertiesFile
							.parse(capabilitiesFile);
					List<Section> sections = capabilitesProperties
							.getSections();
					for (Section section : sections) {
						String capabilityName = section.getName();
						for (Entry entry : section.getEntries()) {
							String key = entry.getKey();
							String[] fileAndFunc = key.split("::", 2);
							if (fileAndFunc.length == 2) {
								String file = fileAndFunc[0].trim();
								URI uri = new File(directory, file).toURI();
								String func = fileAndFunc[1].trim();
								if ("*".equals(func)) {
									addFileCapabilities(uri, capabilityName);
								} else {
									addFunctionCapabilities(uri, func,
											capabilityName);
								}

							}
						}
					}
				}
			}
		} catch (Exception e) {
			CoreMoSyncPlugin.getDefault().log(e);
		}
	}

	private void addFunctionCapabilities(URI uri, String func,
			String capabilityName) {
		Pair capabilitiesKey = new Pair(uri, func);
		Capabilities capabilities = capabilitiesMap.get(capabilitiesKey);
		if (capabilities == null) {
			capabilities = new Capabilities();
			capabilitiesMap.put(capabilitiesKey, capabilities);
		}
		if (capabilityName != null) {
			capabilities.setCapability(capabilityName);
		}
	}

	private void addFileCapabilities(URI uri, String capabilityName) {
		Capabilities capabilities = fileCapabilitiesMap.get(uri);
		if (capabilities == null) {
			capabilities = new Capabilities();
			fileCapabilitiesMap.put(uri, capabilities);
		}
		if (capabilityName != null) {
			capabilities.setCapability(capabilityName);
		}
	}

	public ICapabilities get(URI location, String refid) {
		File file = new File(location);
		File parent = file.getParentFile();
		initCapabilitesMapForDirectory(parent);
		Capabilities funcCapabilities = capabilitiesMap.get(new Pair<URI, String>(location, refid));
		Capabilities fileCapabilities = fileCapabilitiesMap.get(location);
		if (funcCapabilities != null || fileCapabilities != null) {
			return new MergeCapabilities(funcCapabilities, fileCapabilities);
		}

		return null;
	}

}
