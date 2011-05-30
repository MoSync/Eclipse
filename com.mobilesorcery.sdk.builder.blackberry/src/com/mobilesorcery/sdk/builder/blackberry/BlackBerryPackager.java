package com.mobilesorcery.sdk.builder.blackberry;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.builder.java.JavaPackager;
import com.mobilesorcery.sdk.builder.java.KeystoreCertificateInfo;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PreferenceStorePropertyOwner;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.Version;

public class BlackBerryPackager extends JavaPackager {

	public static final String ID = "com.mobilesorcery.sdk.builder.blackberry";

	@Override
	public void createPackage(MoSyncProject project,
			IBuildVariant variant, IBuildResult buildResult)
			throws CoreException {
		// Create a MIDlet
		super.createPackage(project, variant, buildResult, false);
		File jar = buildResult.getBuildResult();
		// We null the build result in case of error
		buildResult.setBuildResult(null);
		
		// Convert the MIDlet to a cod file
		String platform = variant.getProfile().getPlatform();
		JDE jde = matchingJDE(platform);
		if (jde == null) {
			throw new CoreException(new Status(IStatus.ERROR, BlackBerryPlugin.PLUGIN_ID, "Found no matching JDE for Blackberry platform " + platform));
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
	}

	private boolean shouldSign(MoSyncProject project) {
		return PropertyUtil.getBoolean(project, BlackBerryPlugin.PROPERTY_SHOULD_SIGN);
	}

	private JDE matchingJDE(String platform) {
		// TODO: Not correct?
		Version version = new Version(new Path(platform).lastSegment());
		return BlackBerryPlugin.getDefault().getCompatibleJDE(version);
	}

}
