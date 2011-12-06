package com.mobilesorcery.sdk.builder.blackberry;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.builder.java.JavaPackager;
import com.mobilesorcery.sdk.builder.java.KeystoreCertificateInfo;
import com.mobilesorcery.sdk.core.CommandLineBuilder;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.ParameterResolverException;
import com.mobilesorcery.sdk.core.PreferenceStorePropertyOwner;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.Version;
import com.mobilesorcery.sdk.profiles.IProfile;

public class BlackBerryPackager extends JavaPackager {

	public static final String ID = "com.mobilesorcery.sdk.builder.blackberry";

	@Override
	protected void addPlatformSpecifics(MoSyncProject project, IBuildVariant variant, CommandLineBuilder commandLine) throws Exception {
		if (project.getProfileManagerType() != MoSyncTool.DEFAULT_PROFILE_TYPE) {
			throw new CoreException(new Status(IStatus.OK, BlackBerryPlugin.PLUGIN_ID,
					"Can only package BlackBerry with platform-based profiles"));
		}

		JDE jde = matchingJDE(JDE.TYPE_DEV_TOOLS, project, variant.getProfile());
		if (jde == null) {
			throw new CoreException(new Status(IStatus.ERROR, BlackBerryPlugin.PLUGIN_ID, "Found no matching JDE for this Blackberry platform. " +
					"Please note that "));
		}

		commandLine.flag("--blackberry-jde").with(jde.getLocation().toFile());

		if (shouldSign(project)) {
			// We just reuse this java cert info, it's not quite blackberry-ish...
			KeystoreCertificateInfo certInfo = KeystoreCertificateInfo.loadOne(
					BlackBerryPlugin.BLACKBERRY_SIGNING_INFO, new PreferenceStorePropertyOwner(BlackBerryPlugin.getDefault().getPreferenceStore()),
							CoreMoSyncPlugin.getDefault().getSecureProperties());
			String signFileKey = (certInfo == null ? null : certInfo.getKeyPassword());
			if (Util.isEmpty(signFileKey)) {
				throw new CoreException(new Status(IStatus.OK, BlackBerryPlugin.PLUGIN_ID, "No key password for blackberry signing. Please note that for security reasons, passwords are locally stored. You may need to set the password in the BlackBerry preference page."));
			}
			commandLine.flag("--blackberry-signkey").with(signFileKey);
		}
	}

	@Override
	public File computeBuildResult(MoSyncProject project, IBuildVariant variant) throws ParameterResolverException {
		File result = super.computeBuildResult(project, variant);
		String cod = Util.replaceExtension(result.getAbsolutePath(), "cod");
		return new File(cod);
	}

	/*@Override
	public void createPackage(MoSyncProject project, IBuildSession session,
			IBuildVariant variant, IFileTreeDiff diff, IBuildResult buildResult)
			throws CoreException {

		// Create a MIDlet
		super.createPackage(project, session, variant, diff, buildResult);
		File jar = buildResult.getBuildResult();
		// We null the build result in case of error
		buildResult.setBuildResult(null);

		// Convert the MIDlet to a cod file
		//String platform = variant.getProfile().getRuntime();
		JDE jde = matchingJDE(project, variant.getProfile());
		if (jde == null) {
			throw new CoreException(new Status(IStatus.ERROR, BlackBerryPlugin.PLUGIN_ID, "Found no matching JDE for Blackberry platform " + variant.getProfile()));
		}

		// We'll just replace the original jar
		File preverifiedJar = jar;
		try {
			jde.preverifyJAR(jar, preverifiedJar);
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, BlackBerryPlugin.PLUGIN_ID, "Could not preverify", e));
		}

		File finalOutput = new File(jar.getParentFile(), Util.getNameWithoutExtension(jar) + ".cod");
		// TODO: Jad location should not be implicit...
		File jad = new File(jar.getParentFile(), Util.getNameWithoutExtension(jar) + ".jad");
		try {
			jde.convertJARToCOD(preverifiedJar, jad, finalOutput);
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, BlackBerryPlugin.PLUGIN_ID, "Could not convert to COD format (BlackBerry)", e));
		}

		if (shouldSign(project)) {
			// At this point we only support ONE bb cert
			// We just reuse this java cert info, it's not quite blackberry-ish...
			KeystoreCertificateInfo certInfo = KeystoreCertificateInfo.loadOne(
					BlackBerryPlugin.BLACKBERRY_SIGNING_INFO, new PreferenceStorePropertyOwner(BlackBerryPlugin.getDefault().getPreferenceStore()),
							CoreMoSyncPlugin.getDefault().getSecureProperties());
			if (Util.isEmpty(certInfo.getKeyPassword())) {
				throw new CoreException(new Status(IStatus.OK, BlackBerryPlugin.PLUGIN_ID, "No key password for blackberry signing. Please note that for security reasons, passwords are locally stored. You may need to set the password in the BlackBerry preference page."));
			}

			// Sign it
			try {
				jde.sign(finalOutput, certInfo);
			} catch (IOException e) {
				throw new CoreException(new Status(IStatus.ERROR, BlackBerryPlugin.PLUGIN_ID, "Could not sign BlackBerry app", e));
			}
		}

		buildResult.setBuildResult(finalOutput);
	}*/

	private boolean shouldSign(MoSyncProject project) {
		return PropertyUtil.getBoolean(project, BlackBerryPlugin.PROPERTY_SHOULD_SIGN);
	}

	public static JDE matchingJDE(int type, MoSyncProject project, IProfile profile) {
		Version version = new Version(profile.getName());
		if (version.getMajor() == Version.UNDEFINED) {
			throw new IllegalArgumentException(
				"BlackBerry profiles must have a version number as name (eg 4.1)");
		}
		if (type == JDE.TYPE_DEV_TOOLS) {
			return BlackBerryPlugin.getDefault().getCompatibleJDE(version, false);
		} else {
			return BlackBerryPlugin.getDefault().getCompatibleSimulator(version, false);
		}
	}

	public static boolean isSigningRequired(MoSyncProject project) {
		// Yep, it seems that not even PIM requires this?
		return false;
	}
}
