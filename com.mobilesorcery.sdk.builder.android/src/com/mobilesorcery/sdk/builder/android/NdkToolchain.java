package com.mobilesorcery.sdk.builder.android;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import org.eclipse.core.runtime.IPath;

import com.mobilesorcery.sdk.core.AbstractTool;
import com.mobilesorcery.sdk.core.Version;

public class NdkToolchain {

	private IPath root;
	private String arch;
	private Version version;

	public NdkToolchain(IPath root, String arch, Version version) {
		this.root = root;
		this.arch = arch;
		this.version = version;
	}
	
	public boolean isCompatible() {
		// We only support arm
		return arch.contains("arm");
	}

	public Version getVersion() {
		return version;
	}
	
	public String getPreferredPrebuilt() {
		List<String> prebuilts = getPrebuilts();
		TreeMap<Integer, String> prebuiltScores = new TreeMap<Integer, String>();
		for (String prebuilt : prebuilts) {
			int prebuiltScore = 0;
			if (prebuilts.contains("win") && AbstractTool.isWindows()) {
				prebuiltScore += 1;
			}
			if (prebuilts.contains("darwin") && AbstractTool.isMac()) {
				prebuiltScore += 1;
			}
			if (!prebuilt.contains("_64")) {
				prebuiltScore += 1;
			}
			prebuiltScores.put(prebuiltScore, prebuilt);
		}
		
		return prebuiltScores.isEmpty() ? null : prebuiltScores.lastEntry().getValue();
	}
	
	public List<String> getPrebuilts() {
		File prebuiltRoot = root.append("prebuilt").toFile();
		if (!prebuiltRoot.isDirectory()) {
			return Collections.EMPTY_LIST;
		}
		return Arrays.asList(prebuiltRoot.list());
	}

	public IPath getTool(String prebuilt, String tool) {
		if (prebuilt == null) {
			prebuilt = getPreferredPrebuilt();
		}
		if (prebuilt == null) {
			return null;
		}
		return root.append("prebuilt").append(prebuilt).append("bin").append(arch + "-" + tool);
	}

}
