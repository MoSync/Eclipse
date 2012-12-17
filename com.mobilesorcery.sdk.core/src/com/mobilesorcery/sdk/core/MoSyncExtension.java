package com.mobilesorcery.sdk.core;

import java.io.IOException;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.mobilesorcery.sdk.core.SectionedPropertiesFile.Section;

public class MoSyncExtension {

	private String name;
	private Version version = new Version("1.0.0");

	MoSyncExtension(String name) {
		this.name = name;
	}
	
	static MoSyncExtension findExtension(String filename) {
		MoSyncExtension potentialFind = new MoSyncExtension(filename);
		if (potentialFind.getExtensionRoot().toFile().exists()) {
			if (potentialFind.parseManifest()) {
				return potentialFind;
			}
		}
		return null;
	}
	
	private boolean parseManifest() {
		IPath manifestFile = getExtensionRoot().append(new Path("extension.mf"));
		if (!manifestFile.toFile().exists()) {
			return true;
		}
		try {
			SectionedPropertiesFile manifestProperties = SectionedPropertiesFile.parse(manifestFile.toFile());
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
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public String getName() {
		return name;
	}
	
	public Version getVersion() {
		return version;
	}

	public IPath getExtensionRoot() {
		 return MoSyncTool.getDefault().getMoSyncExtensions().append(new Path(name)); 
	}
	
	public IPath getIncludePath() {
		return getExtensionRoot().append("inc");
	}
	
	public IPath getLibPath() {
		return getExtensionRoot().append("lib");
	}
}
