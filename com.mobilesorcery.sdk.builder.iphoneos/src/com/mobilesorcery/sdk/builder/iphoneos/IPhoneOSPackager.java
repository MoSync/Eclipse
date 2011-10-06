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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.AbstractPackager;
import com.mobilesorcery.sdk.core.CommandLineBuilder;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IconManager;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.PackageToolPackager;
import com.mobilesorcery.sdk.core.ParameterResolverException;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.Version;



/**
 * Plugin entry point. Is responsible for creating xcode
 * project for iphone os devices.
 *
 * @author Ali Mosavian
 */
public class IPhoneOSPackager extends PackageToolPackager
{
	public final static String ID = "com.mobilesorcery.sdk.build.ios.packager";

    @Override
	public void createPackage(MoSyncProject project, IBuildVariant variant, IBuildResult buildResult)
    throws CoreException
    {
        try
        {
        	DefaultPackager intern = new DefaultPackager(project, variant);

            super.createPackage(project, variant, buildResult);
            File out = computeBuildResult(project, intern, variant);
            buildResult.setBuildResult(out);

            // Notify user if we did not build the generated project and say why
            if (!XCodeBuild.isMac()) {
            	intern.getConsole().addMessage("Xcode building only available in Mac OS X, will not build generated project");
            } else if (!shouldBuildWithXcodePref()) {
            	intern.getConsole().addMessage("Xcode building disabled, will not build generated project");
            } else if (!XCodeBuild.getDefault().isValid()) {
        		intern.getConsole().addMessage("No Xcode, will not build generated project");
            }
        }
        catch (Exception e) {
        	buildResult.addError(e.getMessage());
        }
    }

    private boolean shouldBuildWithXcodePref() {
    	return !Activator.getDefault().getPreferenceStore().getBoolean(Activator.ONLY_GENERATE_XCODE_PROJECT);
    }

	private boolean shouldBuildWithXcode(MoSyncProject project, IBuildVariant variant) throws CoreException {
		boolean isSimulatorSDK = false;
		boolean isValid = XCodeBuild.getDefault().isValid();
		if (isValid) {
			SDK sdk = getSDK(project, variant);
			isSimulatorSDK = sdk != null && sdk.isSimulatorSDK();
		}
        boolean shouldBuild = isValid && (isSimulatorSDK || shouldBuildWithXcodePref());
        return shouldBuild;
	}

	private File computeBuildResult(MoSyncProject project, DefaultPackager packager, IBuildVariant variant) throws CoreException {
		File xcodeProject = packager.resolveFile( "%package-output-dir%/xcode-proj" );
		if (shouldBuildWithXcode(project, variant)) {
			// Kind of hard-coded in the XCode project template.
			String cfgId = variant.getConfigurationId();
			IBuildConfiguration cfg = project.getBuildConfiguration(cfgId);
			boolean isDebugBuild = cfg != null && cfg.getTypes().contains(IBuildConfiguration.DEBUG_TYPE);
			String target = isDebugBuild ? "Debug" : "Release";
			SDK sdk = getSDK(project, variant);

			String xcodeTarget = target + "-" + sdk.getSDKType();
			// Hm, is this always true...?
			return packager.resolveFile(xcodeProject.getAbsolutePath() + "/build/" + xcodeTarget + "/%app-name%.app");
		} else {
			return xcodeProject;
		}
	}

	private boolean isSimulatorBuild(IBuildVariant variant) {
		return variant.getSpecifiers().containsKey(Activator.IOS_SIMULATOR_SPECIFIER);
	}

	private SDK getSDK(MoSyncProject project, IBuildVariant variant) throws CoreException {
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
        String cert = PropertyUtil.getBoolean(project, PropertyInitializer.IPHONE_PROJECT_SPECIFIC_CERT) ?
        		project.getProperty(PropertyInitializer.IPHONE_CERT):
        		Activator.getDefault().getPreferenceStore().getString(PropertyInitializer.IPHONE_CERT);
        commandLine.flag("--ios-cert").with(cert);

    	String version = internal.get(DefaultPackager.APP_VERSION);
		String ver = new Version(version).asCanonicalString(Version.MICRO);
    	commandLine.flag("--version").with(ver);

    	if (!shouldBuildWithXcode(project, variant)) {
    		commandLine.flag("--ios-project-only");
    	} else {
    		SDK sdk = getSDK(project, variant);
    		commandLine.flag("--ios-sdk").with(sdk.getId());
    	}

    	commandLine.flag("--cpp-output").with(internal.resolveFile("%program-output%").getParent());
	}
}
