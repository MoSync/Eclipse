/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.builder.iphoneos;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.mobilesorcery.sdk.core.BuildResult;
import com.mobilesorcery.sdk.core.CommandLineBuilder;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.IProcessConsole;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PackageToolPackager;
import com.mobilesorcery.sdk.core.ParameterResolverException;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.Version;
import com.mobilesorcery.sdk.profiles.IProfile;



/**
 * Plugin entry point. Is responsible for creating xcode
 * project for iphone os devices.
 *
 * @author Ali Mosavian
 */
public class IPhoneOSPackager extends PackageToolPackager
{
	public final static String ID = "com.mobilesorcery.sdk.build.ios.packager";

	private static final String PROJECT_FILE = "project";

	@Override
	public void createPackage(MoSyncProject project, IBuildSession session,
			IBuildVariant variant, IFileTreeDiff diff, IBuildResult buildResult)
			throws CoreException {
        try
        {
        	DefaultPackager intern = new DefaultPackager(project, variant);

            super.createPackage(project, session, variant, diff, buildResult);
            
            if (shouldUseProvisioning(project, variant)) {
            	executeProvisioning(intern, project, variant);
            }
            
            buildResult.setBuildResult(computeBuildResult(project, variant));

            // Notify user if we did not build the generated project and say why
            if (!XCodeBuild.isMac()) {
            	intern.getConsole().addMessage("Xcode building only available in Mac OS X, will not build generated project");
            } else if (!shouldBuildWithXcodePref() && !isSimulatorBuild(variant)) {
            	intern.getConsole().addMessage("Xcode building disabled, will not build generated project");
            } else if (!XCodeBuild.getDefault().isValid()) {
        		intern.getConsole().addMessage("No Xcode, will not build generated project");
            }
        }
        catch (Exception e) {
        	buildResult.addError(e.getMessage());
        }
    }

    private void executeProvisioning(DefaultPackager intern, MoSyncProject project, IBuildVariant variant) throws IOException, CoreException {
		CommandLineBuilder commandLine = new CommandLineBuilder("xcrun", true);
		SDK sdk = getSDK(project, variant);
		commandLine.flag("-sdk").with(sdk.getId());
		commandLine.flag("PackageApplication");
		File appFile = getAppFile(project, variant);
		commandLine.flag("-v").with(appFile);
		File ipaFile = getIpaFile(project, variant);
		commandLine.flag("-o").with(ipaFile);
		commandLine.flag("--sign").with(getCertificate(project, variant));
		commandLine.flag("--embed").with(project.getProperty(PropertyInitializer.IOS_PROVISIONING_FILE));
		intern.runCommandLine(commandLine.asArray(), commandLine.toHiddenString());
	}

	@Override
    public String getGenerateMode(IProfile profile) {
    	return BUILD_GEN_CPP_MODE;
    }

    private static boolean shouldBuildWithXcodePref() {
    	return !Activator.getDefault().getPreferenceStore().getBoolean(Activator.ONLY_GENERATE_XCODE_PROJECT);
    }

	private static boolean shouldBuildWithXcode(MoSyncProject project, IBuildVariant variant) throws CoreException {
		boolean isSimulatorSDK = false;
		boolean isValid = XCodeBuild.getDefault().isValid();
		if (isValid) {
			SDK sdk = getSDK(project, variant);
			isSimulatorSDK = sdk != null && sdk.isSimulatorSDK();
		}
        boolean shouldBuild = isValid && (isSimulatorSDK || shouldBuildWithXcodePref());
        return shouldBuild;
	}

	@Override
	protected Map<String, List<File>> computeBuildResult(MoSyncProject project, IBuildVariant variant) throws CoreException {
		File xcodeProject = getXcodeProject(project, variant);
		if (shouldBuildWithXcode(project, variant)) {
			File outputAppFile = getAppFile(project, variant);
			File appFile = null;
			if (shouldUseProvisioning(project, variant)) {
				appFile = outputAppFile;
				outputAppFile = getIpaFile(project, variant);
			}
			Map<String, List<File>> buildResult = createBuildResult(outputAppFile);
			if (appFile != null) {
				buildResult.put("appfile", Arrays.asList(appFile));
			}
			return buildResult;
		} else {
			Map<String, List<File>> buildResult = new HashMap<String, List<File>>();
			buildResult.put(PROJECT_FILE, Arrays.asList(xcodeProject));
			return buildResult;
		}
	}
	
	private File getIpaFile(MoSyncProject project, IBuildVariant variant) throws CoreException {
		File appFile = getAppFile(project, variant);
		return new File(Util.replaceExtension(appFile.getAbsolutePath(), "ipa"));		
	}

	private File getAppFile(MoSyncProject project, IBuildVariant variant) throws CoreException {
		File xcodeProject = getXcodeProject(project, variant);
		DefaultPackager packager = new DefaultPackager(project, variant);
		String target = getXcodeTarget(project, variant);
		SDK sdk = getSDK(project, variant);
		String xcodeTarget = target + "-" + sdk.getSDKType();
		String appFileName = packager.resolve("/%app-name%.app").replace(" ", "");

		// Hm, is this always true...?
		File appFile = packager.resolveFile(xcodeProject.getAbsolutePath() + "/build/" + xcodeTarget + appFileName);
		return appFile;
	}

	private File getXcodeProject(MoSyncProject project, IBuildVariant variant) {
		DefaultPackager packager = new DefaultPackager(project, variant);
		File xcodeProject = packager.resolveFile( "%package-output-dir%/xcode-proj" );
		return xcodeProject;
	}

	public static boolean shouldUseProvisioning(MoSyncProject project, IBuildVariant variant) throws CoreException {
		return XCodeBuild.isMac() && 
				shouldBuildWithXcode(project, variant) && 
				!isSimulatorBuild(variant) &&
				!Util.isEmpty(project.getProperty(PropertyInitializer.IOS_PROVISIONING_FILE));
	}

	private String getXcodeTarget(MoSyncProject project, IBuildVariant variant) {
		// Kind of hard-coded in the XCode project template.
		String cfgId = variant.getConfigurationId();
		IBuildConfiguration cfg = project.getBuildConfiguration(cfgId);
		boolean isDebugBuild = cfg != null && cfg.getTypes().contains(IBuildConfiguration.DEBUG_TYPE);
		String target = isDebugBuild ? "Debug" : "Release";
		return target;
	}

	private static boolean isSimulatorBuild(IBuildVariant variant) {
		return variant.getSpecifiers().containsKey(Activator.IOS_SIMULATOR_SPECIFIER);
	}

	private static SDK getSDK(MoSyncProject project, IBuildVariant variant) throws CoreException {
		int sdkType = isSimulatorBuild(variant) ? XCodeBuild.IOS_SIMULATOR_SDKS : XCodeBuild.IOS_SDKS;
		SDK sdk = Activator.getDefault().getSDK(project, sdkType);
		if (sdk == null) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "No simulator SDK found, cannot build"));
		}
		return sdk;
	}

	@Override
	protected void addPlatformSpecifics(MoSyncProject project,
			IBuildVariant variant, CommandLineBuilder commandLine) throws Exception {
		DefaultPackager internal = new DefaultPackager(project, variant);

        // We do not yet support configuration specific certs.
        String cert = getCertificate(project, variant);
        commandLine.flag("--ios-cert").with(cert);

    	String version = internal.get(DefaultPackager.APP_VERSION);
		String ver = new Version(version).asCanonicalString(Version.MICRO);
    	commandLine.flag("--version").with(ver);

    	String bundleId = project.getProperty(PropertyInitializer.IOS_BUNDLE_IDENTIFIER);
    	commandLine.flag("--ios-bundle-id").with(bundleId);
    	IMessageProvider message = Activator.getDefault().validateBundleIdentifier(internal.resolve(bundleId), false);
    	if (message.getMessageType() == IMessageProvider.ERROR) {
    		throw new IllegalArgumentException(message.getMessage());
    	} else if (message.getMessageType() == IMessageProvider.WARNING) {
    		getBuildConsole().addMessage(IProcessConsole.ERR, MessageFormat.format("WARNING: {0}", message.getMessage()));
    	}
    	
    	
    	if (!shouldBuildWithXcode(project, variant)) {
    		commandLine.flag("--ios-project-only");
    	} else {
    		SDK sdk = getSDK(project, variant);
    		commandLine.flag("--ios-sdk").with(sdk.getId());
    		String target = getXcodeTarget(project, variant);
    		commandLine.flag("--ios-xcode-target").with(target);
    	}

    	commandLine.flag("--cpp-output").with(internal.resolveFile("%program-output%").getParent());
	}

	public static String getCertificate(MoSyncProject project, IBuildVariant variant) {
		return PropertyUtil.getBoolean(project, PropertyInitializer.IPHONE_PROJECT_SPECIFIC_CERT) ?
        		project.getProperty(PropertyInitializer.IPHONE_CERT):
        		Activator.getDefault().getPreferenceStore().getString(PropertyInitializer.IPHONE_CERT);
	}
	
	protected void addNativePlatformSpecifics(MoSyncProject project,
			IBuildVariant variant, CommandLineBuilder commandLine) throws Exception {
		SDK sdk = getSDK(project, variant);
		commandLine.flag("--ios-sdk").with(sdk.getId());
		commandLine.flag("--ios-sdk-version").with(sdk.getVersion().asCanonicalString());
		String target = getXcodeTarget(project, variant);
		commandLine.flag("--ios-xcode-target").with(target);
		commandLine.flag("--ios-xcode-location").with(XCodeSelect.getInstance().getCurrentXCodePath());
	}
	
	protected boolean supportsOutputType(String outputType) {
		return super.supportsOutputType(outputType) || MoSyncBuilder.OUTPUT_TYPE_NATIVE_COMPILE.equals(outputType);
	}
}
