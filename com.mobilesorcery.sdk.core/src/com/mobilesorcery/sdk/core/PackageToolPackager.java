package com.mobilesorcery.sdk.core;

import java.io.File;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.apisupport.nfc.NFCSupport;
import com.mobilesorcery.sdk.core.build.AbstractBuildStep;
import com.mobilesorcery.sdk.core.security.IApplicationPermissions;
import com.mobilesorcery.sdk.core.security.ICommonPermissions;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.Profile;

/**
 * Uses the packager tool in the mosync binary directory to create packages
 *
 * @author mattias.bybro@mosync.com
 *
 */
public abstract class PackageToolPackager extends AbstractPackager {

	@Override
	public void createPackage(MoSyncProject project, IBuildSession session,
			IBuildVariant variant, IFileTreeDiff diff, IBuildResult buildResult)
			throws CoreException {
		IProfile profile = variant.getProfile();

		try {
			if (needsPackaging(diff)) {
				IPath packagerTool = MoSyncTool.getDefault().getBinary(
						"package");

				DefaultPackager internal = new DefaultPackager(project, variant);

				CommandLineBuilder commandLine = new CommandLineBuilder(
						packagerTool.toOSString());
				addGeneralParameters(project, session, variant, commandLine);
				addPlatformSpecifics(project, variant, commandLine);

				String packageOutputDirStr = internal
						.get(DefaultPackager.PACKAGE_OUTPUT_DIR);
				File packageOutputDir = new File(packageOutputDirStr);
				packageOutputDir.mkdirs();

				internal.runCommandLine(commandLine.asArray(),
						commandLine.toHiddenString());
			}
			buildResult.setBuildResult(computeBuildResult(project, variant));
		} catch (Exception e) {
			String errorMsg = MessageFormat.format(
					"Failed to create package for {0} (platform: {1}). Reason: {2}",
					profile, Profile.getAbbreviatedPlatform(profile), e.getMessage());
			throw new CoreException(new Status(IStatus.ERROR,
					CoreMoSyncPlugin.PLUGIN_ID, errorMsg, e));
		}
	}

	private boolean needsPackaging(IFileTreeDiff diff) {
		// Poor man's dependency check -- if any file is touched with the
		// exception
		// of .* resources, then rebuild. This is because we have no dependency
		// file output from the package tool (yet?)
		if (diff == null) {
			return true;
		}

		Collection<IPath> changeSet = diff.getChanged();
		for (IPath changed : changeSet) {
			if (!changed.lastSegment().startsWith(".")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Clients override this method and/or the createPackage method.
	 *
	 * @param variant
	 * @param project
	 * @return
	 * @throws ParameterResolverException
	 * @throws CoreException
	 */
	protected Map<String, List<File>> computeBuildResult(MoSyncProject project,
			IBuildVariant variant) throws ParameterResolverException, CoreException {
		return null;
	}

	private void addGeneralParameters(MoSyncProject project, IBuildSession session,
			IBuildVariant variant, CommandLineBuilder commandLine)
			throws Exception {
		DefaultPackager internal = new DefaultPackager(project, variant);

		IProfile profile = variant.getProfile();

		File program = internal.resolveFile("%program-output%");
		File resource = internal.resolveFile("%resource-output%");
		File iconFile = project.getIconFile();
		if (iconFile == null) {
			iconFile = getDefaultIconFile();
		}
		String packageOutputDir = internal
				.get(DefaultPackager.PACKAGE_OUTPUT_DIR);
		String vendor = internal.get(DefaultPackager.APP_VENDOR_NAME);
		Version version = new Version(internal.get(DefaultPackager.APP_VERSION));
		String appName = internal.get(DefaultPackager.APP_NAME);
		IApplicationPermissions permissions = project.getPermissions();
		
		Object modifiedPermissions = session.getProperties().get(AbstractBuildStep.MODIFIED_PERMISSIONS);
		if (modifiedPermissions instanceof IApplicationPermissions) {
			permissions = (IApplicationPermissions) modifiedPermissions;
		}
		
		String permissionsStr = Util.join(
				permissions.getRequestedPermissions(true).toArray(), ",");

		if (project.getProfileManagerType() == MoSyncTool.DEFAULT_PROFILE_TYPE) {
			commandLine.flag("-t").with("platform");
		}

		commandLine.flag("-p").with(program);
		if (resource.exists()) {
			commandLine.flag("-r").with(resource);
		}
		if (iconFile != null && iconFile.exists()) {
			commandLine.flag("-i").with(iconFile);
		}

		commandLine.flag("-d").with(packageOutputDir).flag("-m")
				.with(MoSyncTool.toString(profile)).flag("--vendor")
				.with(vendor).flag("-n").with(appName).flag("--version")
				.with(version.asCanonicalString());

		boolean useStaticRecompile = shouldUseStaticRecompile(project, variant);

		String[] extensions = PropertyUtil.getStrings(MoSyncBuilder.getPropertyOwner(project, variant.getConfigurationId()), MoSyncBuilder.EXTENSIONS);
		if (extensions.length > 0) {
			commandLine.flag("--extensions").with(Util.join(extensions, ","));
		}
		
		if (!useStaticRecompile) {
			commandLine.flag("--output-type").with("interpreted");
		}

		if (!Util.isEmpty(permissionsStr)) {
			commandLine.flag("--permissions").with(permissionsStr);
		}

		if (shouldUseDebugRuntimes(project, variant)) {
			commandLine.flag("--debug");
		}

		if (permissions.isPermissionRequested(ICommonPermissions.NFC)) {
			File nfcDescription = NFCSupport.create(project)
					.getNFCDescription();
			commandLine.flag("--nfc").with(nfcDescription);
		}

	}

	protected boolean shouldUseStaticRecompile(MoSyncProject project, IBuildVariant variant) {
		return PropertyUtil.getBoolean(
				MoSyncBuilder.getPropertyOwner(project,
						variant.getConfigurationId()),
				MoSyncBuilder.USE_STATIC_RECOMPILATION);
	}

	protected File getDefaultIconFile() {
		File iconFile = MoSyncTool.getDefault().getMoSyncHome()
				.append("etc/default.icon").toFile();
		return iconFile.exists() ? iconFile : null;
	}

	protected void addPlatformSpecifics(MoSyncProject project,
			IBuildVariant variant, CommandLineBuilder commandLine)
			throws Exception {

	}

	protected Map<String, List<File>> createBuildResult(File file) {
		HashMap<String, List<File>> result = new HashMap<String, List<File>>();
		result.put(IBuildResult.MAIN, Arrays.asList(file));
		return result;
	}

	protected IProcessConsole getBuildConsole() {
		return CoreMoSyncPlugin.getDefault().createConsole(MoSyncBuilder.CONSOLE_ID);
	}
}
