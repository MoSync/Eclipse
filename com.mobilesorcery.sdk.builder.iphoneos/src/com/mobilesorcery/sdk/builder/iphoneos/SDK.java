package com.mobilesorcery.sdk.builder.iphoneos;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mobilesorcery.sdk.core.Version;

public class SDK {

	private final static Pattern VERSION_PATTERN = Pattern.compile("(\\D*)(.*)");

	private final String name;

	private final String id;

	private Version version;

	private String sdkType;

	SDK(String id, String name) {
		this.name = name;
		this.id = id;
		computeVersionAndSDKType(id);
	}

	private void computeVersionAndSDKType(String id) {
		Matcher matcher = VERSION_PATTERN.matcher(id);
		if (matcher.matches()) {
			String versionStr = matcher.group(2);
			version = new Version(versionStr).truncate(Version.MINOR);
			sdkType = matcher.group(1).trim();
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

	public String getSDKType() {
		return sdkType;
	}

}
