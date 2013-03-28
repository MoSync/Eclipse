package com.mobilesorcery.sdk.builder.winmobilecs;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.util.Util;

import com.mobilesorcery.sdk.core.CommandLineBuilder;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PackageToolPackager;
import com.mobilesorcery.sdk.profiles.IProfile;

public class WinMobileCSPackager extends PackageToolPackager {

	public static final String ID = "com.mobilesorcery.sdk.build.winmobilecs.packager";

	private static final String PROJECT_FILE = "project";

	@Override
	public void createPackage(MoSyncProject project, IBuildSession session,
			IBuildVariant variant, IFileTreeDiff diff, IBuildResult buildResult)
			throws CoreException {
		DefaultPackager intern = new DefaultPackager(project, variant);

		super.createPackage(project, session, variant, diff, buildResult);

		// Notify user if we did not build the generated project and say why
		if (!Util.isWindows()) {
			intern.getConsole()
					.addMessage(
							"Visual Studio building only available in Windows, will not build generated project");
		} else if (!shouldBuildWithVSPref(project) && !isEmulatorBuild(project, variant)) {
			intern.getConsole()
					.addMessage(
							"Visual Studio building disabled, will not build generated project");
		} else if (!MSBuild.getDefault().isValid()) {
			intern.getConsole().addMessage("Visual Studio building disabled.");
			intern.getConsole().addMessage("Could not find Visual Studio executable; see Preferences > MoSync Tool > Visual Studio (WP7)");
        }
	}

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
			commandLine.flag("--wp-vs-build-path").with(MSBuild.getDefault().getToolPath());
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
		if (MoSyncBuilder.OUTPUT_TYPE_STATIC_RECOMPILATION.equals(MoSyncBuilder.getPropertyOwner(project, variant.getConfigurationId()).getProperty(MoSyncBuilder.OUTPUT_TYPE))) {
			return isDebugBuild ? "rebuild_debug" : "rebuild_release";
		} else {
			return isDebugBuild ? "Debug" : "Release";
		}
	}

	private boolean shouldBuildWithVS(MoSyncProject project,
			IBuildVariant variant) {
		boolean generateOnly = !Util.isWindows()
				|| !shouldBuildWithVSPref(project);
		return MSBuild.getDefault().isValid() && (!generateOnly || isEmulatorBuild(project, variant));
	}

	private boolean shouldBuildWithVSPref(MoSyncProject project) {
		return WinMobileCSPlugin.getDefault().getPreferenceStore()
				.getBoolean(WinMobileCSPlugin.BUILD_WITH_VS);
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
