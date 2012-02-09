package com.mobilesorcery.sdk.builder.winmobilecs;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.mobilesorcery.sdk.core.AbstractTool;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.Version;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

public class MSBuild extends AbstractTool {

	private static MSBuild instance;
	private static IPath toolPath = null;

	public static MSBuild getDefault() {
		String vsBuildExeStr = WinMobileCSPlugin.getDefault()
				.getPreferenceStore()
				.getString(WinMobileCSPlugin.MS_BUILD_PATH);
		IPath newToolPath = new Path(vsBuildExeStr);
		if (instance == null || !Util.equals(toolPath, newToolPath)) {
			IPath path = newToolPath == null ? null : newToolPath;
			instance = new MSBuild(path);
		}
		return instance;
	}

	protected MSBuild(IPath toolPath) {
		super(toolPath);
	}

	@Override
	protected String getToolName() {
		return "Visual Studio build tool";
	}

	public static MSBuild guess() {
		File fwRoot = new File(WinMobileCSPlugin.getSystemRoot(),
				"/Microsoft.NET/Framework");
		if (fwRoot.isDirectory()) {
			File[] subdirs = fwRoot.listFiles();
			Version bestMatch = null;
			File bestMatchPath = null;
			for (File subdir : subdirs) {
				String subdirName = subdir.getName();
				if (subdir.isDirectory() && subdirName.length() > 1) {
					File msbuildFile = new File(subdir, "MSBuild.exe");
					Version subdirVersion = new Version(subdirName.substring(1));
					if (msbuildFile.exists()
							&& subdirVersion.isValid()
							&& (bestMatch == null || subdirVersion
									.isNewer(bestMatch))) {
						bestMatch = subdirVersion;
						bestMatchPath = msbuildFile;
					}
				}
			}
			if (bestMatchPath != null) {
				if (CoreMoSyncPlugin.getDefault().isDebugging()) {
					CoreMoSyncPlugin.trace(
							"Found MSBuild executable version {0} at {1}",
							bestMatch.asCanonicalString(), bestMatchPath);
				}
				return new MSBuild(new Path(bestMatchPath.getAbsolutePath()));
			}
		}
		return null;
	}

}
