package com.mobilesorcery.sdk.builder.iphoneos;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mobilesorcery.sdk.core.Version;

public class SDK {

	private final static Pattern VERSION_PATTERN = Pattern.compile("\\D*(.*)");
	
	private String name;
	
	private String id;

	private Version version;

	SDK(String id, String name) {
		this.name = name;
		this.id = id;
		computeVersion(id);
	}
	
	private void computeVersion(String id) {
		Matcher matcher = VERSION_PATTERN.matcher(id);
		if (matcher.matches()) {
			String versionStr = matcher.group(1);
			version = new Version(versionStr).truncate(Version.MINOR);
		}
	}

	public String getName() {
		return name;
	}
	
	public String getId() {
		return id;
	}
	
	public boolean isSimulatorSDK() {
		// Some kind of heuristic...
		return name.contains("Simulator");
	}

	public boolean isIOSSDK() {
		// Yet some other kind of heuristic...
		return name.contains("iOS");
	}

	public Version getVersion() {
		return version;
	}

}
