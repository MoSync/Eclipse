package com.mobilesorcery.sdk.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.mobilesorcery.sdk.core.SectionedPropertiesFile.Section;
import com.mobilesorcery.sdk.profiles.IVendor;

public class MoSyncExtension {

	private String name;
	private Version version = new Version("1.0.0");
	private String vendor = "<unknown vendor>";
	private List<IVendor> platforms;

	public MoSyncExtension(String name) {
		this.name = name;
	}
	
	public void setName(String name) {
		if (this.name != null) {
			throw new IllegalStateException("Cannot only set extension name to uninitialized extension");
		}
		this.name = name;
	}
	
	public static MoSyncExtension findExtension(String filename) {
		MoSyncExtension potentialFind = new MoSyncExtension(filename);
		if (potentialFind.getExtensionRoot().toFile().exists()) {
			if (potentialFind.parseManifest() == null) {
				return potentialFind;
			}
		}
		return null;
	}
	
	private String parseManifest() {
		return parseManifest(getExtensionRoot().toFile());
	}
	
	private String parseManifest(File manifestDir) {
		File manifestFile = new File(manifestDir, "extension.mf");
		if (!manifestFile.exists()) {
			return "No manifest file (extension.mf)";
		}
		try {
			SectionedPropertiesFile manifestProperties = SectionedPropertiesFile.parse(manifestFile);
			Section defaultSection = manifestProperties.getDefaultSection();
			Map<String, String> properties = defaultSection.getEntriesAsMap();
			String versionStr = properties.get("version");
			if (versionStr != null) {
				this.version = new Version(versionStr);
			}
			String nameStr = properties.get("name");
			if (nameStr != null) {
				this.name = nameStr;
			}
			String vendorStr = properties.get("vendor");
			if (vendorStr != null) {
				this.vendor = vendorStr;
			}
			String platformNamesStr = properties.get("platforms");
			if (platformNamesStr != null) {
				String[] platformNames = platformNamesStr.split(",");
				ArrayList<IVendor> platforms = new ArrayList<IVendor>();
				ProfileManager mgr = MoSyncTool.getDefault().getProfileManager(MoSyncTool.DEFAULT_PROFILE_TYPE);
				for (String platformName: platformNames) {
					IVendor platform = mgr.getVendor(platformName);
					if (platform != null) {
						platforms.add(platform);
					}
				}
				this.platforms = Collections.unmodifiableList(platforms);
			}
		} catch (Exception e) {
			return "Could not parse manifest file: " + e.getMessage();
		}
		return null;
	}

	public String getName() {
		return name;
	}
	
	public Version getVersion() {
		return version;
	}
	
	public String getVendor() {
		return vendor;
	}
	
	public List<IVendor> getPlatforms() {
		return platforms;
	}

	public IPath getExtensionRoot() {
		 IPath result = MoSyncTool.getDefault().getMoSyncExtensions().append(new Path(name));
		 validateInExtensionsDir(result.toFile());
		 return result;
	}
	
	public IPath getIncludePath() {
		return getExtensionRoot().append("inc");
	}
	
	public IPath getLibPath() {
		return getExtensionRoot().append("lib");
	}
	
	static void validateInExtensionsDir(File installLocation) {
		if (!Util.isParent(MoSyncTool.getDefault().getMoSyncExtensions().toFile(), installLocation)) {
			throw new IllegalStateException("Illegal install location: " + installLocation);
		}
	}

	static void validateInstallable(File installLocation) throws IOException {
		MoSyncExtension throwaway = new MoSyncExtension(null);
		String result = throwaway.parseManifest(installLocation);
		if (result != null) {
			throw new IOException(result);
		}
	}

}
