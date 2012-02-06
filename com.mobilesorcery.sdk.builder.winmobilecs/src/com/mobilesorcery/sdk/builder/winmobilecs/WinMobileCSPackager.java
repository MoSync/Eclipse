package com.mobilesorcery.sdk.builder.winmobilecs;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.util.Util;

import com.mobilesorcery.sdk.core.CommandLineBuilder;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PackageToolPackager;
import com.mobilesorcery.sdk.profiles.IProfile;

public class WinMobileCSPackager extends PackageToolPackager {

	public static final String ID = "com.mobilesorcery.sdk.build.winmobilecs.packager";

	private static final String PROJECT_FILE = "project";

	@Override
	public Map<String, List<File>> computeBuildResult(MoSyncProject project,
			IBuildVariant variant) {
		DefaultPackager internal = new DefaultPackager(project, variant);
		if (shouldBuildWithVS(project, variant)) {
			String config = getConfig(project, variant);
			File xapFile = internal
					.resolveFile("%package-output-dir%/project/bin/" + config
							+ "/%app-name%.xap");
			return createBuildResult(xapFile);
		} else {
			File csProjFile = internal
					.resolveFile("%package-output-dir%/project");
			Map<String, List<File>> buildResult = new HashMap<String, List<File>>();
			buildResult.put(WinMobileCSPackager.PROJECT_FILE,
					Arrays.asList(csProjFile));
			return buildResult;
		}
	}

	@Override
	public void addPlatformSpecifics(MoSyncProject project,
			IBuildVariant variant, CommandLineBuilder commandLine)
			throws CoreException {
		DefaultPackager internal = new DefaultPackager(project, variant);
		commandLine.flag("--cs-output").with(
				internal.resolveFile("%program-output%").getParent());

		if (!shouldBuildWithVS(project, variant)) {
			commandLine.flag("--wp-project-only");
		} else {
			String vsBuildExeStr = WinMobileCSPlugin.getDefault()
					.getPreferenceStore()
					.getString(WinMobileCSPlugin.MS_BUILD_PATH);
			File vsBuildExe = new File(vsBuildExeStr);
			if (com.mobilesorcery.sdk.core.Util.isEmpty(vsBuildExeStr)
					|| !vsBuildExe.exists()) {
				throw new CoreException(
						new Status(
								IStatus.ERROR,
								WinMobileCSPlugin.PLUGIN_ID,
								"Could not find Visual Studio executable; see Preferences > MoSync Tool > Visual Studio (WP7)"));
			}
			commandLine.flag("--wp-vs-build-path").with(vsBuildExe);
		}
		String target = isEmulatorBuild(project, variant) ? "emulator"
				: "device";
		commandLine.flag("--wp-target").with(target);

		String config = getConfig(project, variant);
		commandLine.flag("--wp-config").with(config);

		commandLine.flag("--wp-guid").with(
				project.getProperty(PropertyInitializer.GUID));
	}

	private String getConfig(MoSyncProject project, IBuildVariant variant) {
		IBuildConfiguration cfg = project.getBuildConfiguration(variant
				.getConfigurationId());
		boolean isDebugBuild = cfg != null
				&& cfg.getTypes().contains(IBuildConfiguration.DEBUG_TYPE);
		if (shouldUseStaticRecompile(project, variant)) {
			return isDebugBuild ? "rebuild_debug" : "rebuild_release";
		} else {
			return isDebugBuild ? "Debug" : "Release";
		}
	}

	private boolean shouldBuildWithVS(MoSyncProject project,
			IBuildVariant variant) {
		boolean generateOnly = !Util.isWindows() ||
				WinMobileCSPlugin.getDefault().getPreferenceStore().getBoolean(WinMobileCSPlugin.ONLY_GENERATE_MS_BUILD_PROJECT);
		return !generateOnly || isEmulatorBuild(project, variant);
	}

	private boolean isEmulatorBuild(MoSyncProject project, IBuildVariant variant) {
		return variant.getSpecifiers().containsKey(
				WinMobileCSPlugin.WP_EMULATOR_SPECIFIER);
	}

	@Override
	public String getGenerateMode(IProfile profile) {
		return BUILD_GEN_CS_MODE;
	}

}
