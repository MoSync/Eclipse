package com.mobilesorcery.sdk.builder.android;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;

import com.mobilesorcery.sdk.builder.java.KeystoreCertificateInfo;
import com.mobilesorcery.sdk.core.CommandLineBuilder;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PackageToolPackager;
import com.mobilesorcery.sdk.core.ParameterResolverException;
import com.mobilesorcery.sdk.ui.DefaultMessageProvider;

public class AndroidPackager extends PackageToolPackager {

	public final static String ID = "com.mobilesorcery.sdk.build.android.packager";

	public AndroidPackager() {
		super();
	}

	@Override
	protected void addPlatformSpecifics(MoSyncProject project, IBuildVariant variant, CommandLineBuilder commandLine) throws Exception {
		String packageName = project.getProperty(PropertyInitializer.ANDROID_PACKAGE_NAME);
		String versionCode = project.getProperty(PropertyInitializer.ANDROID_VERSION_CODE);

		KeystoreCertificateInfo keystoreCertInfo = KeystoreCertificateInfo.loadOne(
				PropertyInitializer.ANDROID_KEYSTORE_CERT_INFO,
				PropertyInitializer.ANDROID_PROJECT_SPECIFIC_KEYS,
				project,
				Activator.getDefault().getPreferenceStore());

        if (keystoreCertInfo == null || !DefaultMessageProvider.isEmpty(keystoreCertInfo.validate(false))) {
        	throw new CoreException(new Status(IStatus.OK, Activator.PLUGIN_ID, keystoreCertInfo.validate(false).getMessage()));
        }

        String keystore = keystoreCertInfo.getKeystoreLocation();
        String alias = keystoreCertInfo.getAlias();
        String storepass = keystoreCertInfo.getKeystorePassword();
        String keypass = keystoreCertInfo.getKeyPassword();

        commandLine.flag("--android-package").with(packageName);
        commandLine.flag("--android-version-code").with(versionCode);
        commandLine.flag("--android-keystore").with(keystore);
        commandLine.flag("--android-storepass", true).with(storepass);
        commandLine.flag("--android-alias").with(alias);
        commandLine.flag("--android-keypass", true).with(keypass);
	}

	@Override
	protected File computeBuildResult(MoSyncProject project,
			IBuildVariant variant) throws ParameterResolverException {
		DefaultPackager intern = new DefaultPackager(project, variant);
		return new File(intern.get(DefaultPackager.PACKAGE_OUTPUT_DIR), intern.get(DefaultPackager.APP_NAME) + ".apk");
	}

	/**
	 * Validates a package name (is it a proper android package name?)
	 * @param packageName
	 * @return
	 */
	public static IMessageProvider validatePackageName(String packageName) {
		String[] packageParts = packageName.split("\\.");
		if (packageParts.length < 2) {
			return new DefaultMessageProvider("Android packages must have at least two parts (eg com.test)", IMessageProvider.ERROR);
		}

		for (int i = 0; i < packageParts.length; i++) {
			String error = validatePackagePart(packageParts[i]);
			if (error != null) {
				return new DefaultMessageProvider(error, IMessageProvider.ERROR);
			}
		}

		return DefaultMessageProvider.EMPTY;
	}

	private static String validatePackagePart(String packagePart) {
		char[] packagePartCh = packagePart.toCharArray();
		if (packagePart.length() == 0) {
			return "Package segment cannot be empty";
		}

		char invalidChar = Character.isJavaIdentifierStart(packagePartCh[0]) ? '\0' : packagePartCh[0];

		for (int i = 1; invalidChar == '\0' && i < packagePartCh.length; i++) {
			invalidChar = Character.isJavaIdentifierPart(packagePartCh[i]) ? '\0' : packagePartCh[i];
		}

		if (invalidChar != '\0') {
			return MessageFormat.format("Invalid package character: {0}", invalidChar);
		}

		return null;
	}
}
