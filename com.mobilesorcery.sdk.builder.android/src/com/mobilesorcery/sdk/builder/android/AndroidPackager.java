package com.mobilesorcery.sdk.builder.android;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.mobilesorcery.sdk.builder.android.launch.Android;
import com.mobilesorcery.sdk.builder.java.KeystoreCertificateInfo;
import com.mobilesorcery.sdk.core.AbstractTool;
import com.mobilesorcery.sdk.core.CommandLineBuilder;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.PackageToolPackager;
import com.mobilesorcery.sdk.core.ParameterResolver;
import com.mobilesorcery.sdk.core.ParameterResolverException;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.Util;
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

		ParameterResolver resolver = MoSyncBuilder.createParameterResolver(project, variant);
        if (keystoreCertInfo == null || !DefaultMessageProvider.isEmpty(keystoreCertInfo.validate(false, resolver))) {
        	throw new CoreException(new Status(IStatus.OK, Activator.PLUGIN_ID, keystoreCertInfo.validate(false, resolver).getMessage()));
        }

        String keystore = Util.replace(keystoreCertInfo.getKeystoreLocation(), resolver);
        String alias = keystoreCertInfo.getAlias();
        String storepass = keystoreCertInfo.getKeystorePassword();
        String keypass = keystoreCertInfo.getKeyPassword();

        commandLine.flag("--android-package").with(packageName);
        commandLine.flag("--android-version-code").with(versionCode);
        commandLine.flag("--android-keystore").with(keystore);
        commandLine.flag("--android-storepass", true).with(storepass);
        commandLine.flag("--android-alias").with(alias);
        commandLine.flag("--android-keypass", true).with(keypass);
        
        if (PropertyUtil.getBoolean(project, PropertyInitializer.ANDROID_LARGE_HEAP)) {
        	commandLine.flag("--android-heap").with("large");
        }
        
        String installLocation = project.getProperty(PropertyInitializer.ANDROID_INSTALL_LOCATION);
        if (!Util.isEmpty(installLocation)) {
        	commandLine.flag("--android-install-location").with(installLocation);
        }
        
        String manifestTemplate = project.getProperty(PropertyInitializer.ANDROID_MANIFEST_TEMPLATE);
        if (!Util.isEmpty(manifestTemplate)) {
        	commandLine.flag("--android-manifest-template").with(manifestTemplate);
        }
        
        Android external = Android.getExternal();
        if (external.isValid()) {
        	commandLine.flag("--android-sdk-location").with(Activator.getDefault().getExternalAndroidSDKPath().toFile());
        }
	}

	@Override
	protected Map<String, List<File>> computeBuildResult(MoSyncProject project,
			IBuildVariant variant) throws ParameterResolverException {
		DefaultPackager intern = new DefaultPackager(project, variant);
		return createBuildResult(new File(intern.get(DefaultPackager.PACKAGE_OUTPUT_DIR), intern.get(DefaultPackager.APP_NAME) + ".apk"));
	}
	
	public void buildNative(MoSyncProject project, IBuildSession session, 
			IBuildVariant variant, IBuildResult result) throws Exception {
		File location = project.getWrappedProject().getLocation().toFile();
		IPath dst = MoSyncBuilder.getPackageOutputPath(project.getWrappedProject(), variant).removeLastSegments(1);
		
		if (location.getAbsolutePath().indexOf(' ') != -1 || dst.toFile().getAbsolutePath().indexOf(' ') != -1) {
			throw new IllegalArgumentException(MessageFormat.format(
					"Project or output path cannot have spaces: {0}",
					location.getAbsolutePath()));
		}
		
		super.buildNative(project, session, variant, result);
	}
	
	protected List<File> computeNativeBuildResult(MoSyncProject project,
			IBuildVariant variant) throws ParameterResolverException, CoreException {
		// armeabi and armeabi-v7a
		boolean debug = shouldUseDebugRuntimes(project, variant);
		ArrayList<File> result = new ArrayList<File>();
		File armLib = computeNativeBuildResult(project, variant, "armeabi", debug);
		File armv7aLib = computeNativeBuildResult(project, variant, "armeabi-v7a", debug);
		result.add(armLib);
		result.add(armv7aLib);
		return result;
	}
	
	public static File computeNativeBuildResult(MoSyncProject project,
			IBuildVariant variant, String arch, boolean debug) {
		IPath outputRoot = MoSyncBuilder.getOutputPath(project.getWrappedProject(), variant);
		// Hm, only shared right now.
		String cfgQualifier = debug ? "debug" : "release";
		IPath lib = outputRoot.append("android_" + arch + "_" + cfgQualifier + "/lib" + project.getName() + ".so");
		return lib.toFile();
	}
	
	public static File computeNativeDebugLib(MoSyncProject project, IBuildVariant variant, String arch) {
		File tempBuildDir = getTempBuildDir(project, variant);
		return new File(tempBuildDir, "Debug/obj/local/" + arch + "/lib" + project.getName() + ".so");
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
	
	protected void addNativePlatformSpecifics(MoSyncProject project,
			IBuildVariant variant, CommandLineBuilder commandLine) throws Exception {
		IPreferenceStore androidPrefs = Activator.getDefault().getPreferenceStore();
		String ndkLocation = androidPrefs.getString(Activator.NDK_PATH);
		int platformVersion = androidPrefs.getInt(Activator.NDK_PLATFORM_VERSION);
		if (Util.isEmpty(ndkLocation)) {
			throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, "Missing NDK location"));
		}
		if (platformVersion < 1) {
			throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, "Missing NDK version"));
		}
		commandLine.flag("--android-ndk-location").with(new File(ndkLocation));
		commandLine.flag("--android-version").with(Integer.toString(platformVersion));
		commandLine.flag("--android-build-dir").with(getTempBuildDir(project, variant));
		
		List<String> argumentList = Arrays.asList(Platform.getApplicationArgs());
    	boolean useNdkStl = !AbstractTool.isWindows();
		if (argumentList.contains("-android-stl-support:true")) {
			useNdkStl = true;
		} else if (argumentList.contains("-android-stl-support:false")) {
			useNdkStl = false;
		}
		if (useNdkStl) {
			// There are situations where this does not work;
			// if a project has no explicit link to the STL lib,
			// it will all fail...
			//commandLine.flag("--android-stl-support");	
    	}
	}

	private static File getTempBuildDir(MoSyncProject project, IBuildVariant variant) {
		IPath dst = MoSyncBuilder.getPackageOutputPath(project.getWrappedProject(), variant).removeLastSegments(1);
		return dst.append(new Path("temp")).toFile();
	}

	protected boolean supportsOutputType(String outputType) {
		return super.supportsOutputType(outputType) || MoSyncBuilder.OUTPUT_TYPE_NATIVE_COMPILE.equals(outputType);
	}
	
	protected List<String> getExtensionModules(MoSyncProject project, IBuildVariant variant) {
		// TODO: This should obviously not be hard coded. Order is important too.
		ArrayList<String> result = new ArrayList<String>();
		if (getOutputType(project).equals(MoSyncBuilder.OUTPUT_TYPE_NATIVE_COMPILE)) {
			result.addAll(Arrays.asList(new String[] { 
					"mautil", "yajl", "maui", "mafs",
					"map", "ads", "nativeui", "Facebook", "Purchase",
					"matest", "testify", "Notification", "Wormhole", "MoGraph"
			}));
		}
		
		result.addAll(super.getExtensionModules(project, variant));
		return result;
	}
}
