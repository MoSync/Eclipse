package com.mobilesorcery.sdk.core;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.apisupport.nfc.NFCSupport;
import com.mobilesorcery.sdk.core.security.IApplicationPermissions;
import com.mobilesorcery.sdk.core.security.ICommonPermissions;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.Profile;

/**
 * Uses the packager tool in the mosync binary directory to create
 * packages
 * @author mattias.bybro@mosync.com
 *
 */
public abstract class PackageToolPackager extends AbstractPackager {

	@Override
	public void createPackage(MoSyncProject project,
			IBuildVariant variant, IBuildResult buildResult)
			throws CoreException {
		IProfile profile = variant.getProfile();
		try {
			IProcessConsole console = CoreMoSyncPlugin.getDefault().createConsole(MoSyncBuilder.CONSOLE_ID);
			DefaultPackager internal = new DefaultPackager(project, variant);
			internal.setParameters(getParameters());
			/*
			 -p, --program <file>             Compiled program file.
			 -r, --resource <file>            Compiled resource file.
			 -i, --icon <file>                MoSync icon definition file (XML).
			 -f, --profile <profile name>     Target profile.
			 -d, --dst <path>                 Target directory.
			 -n, --name <name>                Application name.
			     --vendor <name>              Application vendor's name.
			     --version <version>          Application version, e.g. 1.0.0
			     --permissions                Comma separated list of permissions
			     --debug                      Use debug runtime.
			*/
			IPath packagerTool = MoSyncTool.getDefault().getBinary("package");

			File program = internal.resolveFile("%program-output%");
			File resource = internal.resolveFile("%resource-output%");
			File iconFile = project.getIconFile();
			String packageOutputDir = internal.get(DefaultPackager.PACKAGE_OUTPUT_DIR);
			String vendor = internal.get(DefaultPackager.APP_VENDOR_NAME);
			// TODO: version could be different per platform
			Version version = new Version(internal.get(DefaultPackager.APP_VERSION));
			String appName = internal.get(DefaultPackager.APP_NAME);
			IApplicationPermissions permissions = project.getPermissions();
			String permissionsStr = Util.join(permissions.getRequestedPermissions().toArray(), ",");

			CommandLineBuilder commandLine = new CommandLineBuilder(packagerTool.toOSString());

			commandLine.flag("-p").with(program);
			if (resource.exists()) {
				commandLine.flag("-r").with(resource);
			}
			if (iconFile != null && iconFile.exists()) {
				commandLine.flag("-i").with(iconFile);
			}

			commandLine.flag("-d").with(packageOutputDir).flag("-m").with(MoSyncTool.toString(profile)).
			flag("--vendor").with(vendor).flag("-n").with(appName)/*.flag("--version").with(version.asCanonicalString())*/;

			if (!Util.isEmpty(permissionsStr)) {
				commandLine.flag("--permissions").with(permissionsStr);
			}

			if (shouldUseDebugRuntimes()) {
				commandLine.flag("--debug");
			}

			if (permissions.isPermissionRequested(ICommonPermissions.NFC)) {
				File nfcDescription = NFCSupport.create(project).getNFCDescription();
				commandLine.flag("--nfc").with(nfcDescription);
			}

			addPlatformSpecifics(project, variant, commandLine);

			internal.runCommandLine(commandLine.asArray());
		} catch (Exception e) {
			String errorMsg = MessageFormat.format("Failed to create package for {0} (platform: {1})", profile, Profile.getAbbreviatedPlatform(profile));
			throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, errorMsg, e));
		}
	}

	protected abstract void addPlatformSpecifics(MoSyncProject project, IBuildVariant variant, CommandLineBuilder commandLine);

}
