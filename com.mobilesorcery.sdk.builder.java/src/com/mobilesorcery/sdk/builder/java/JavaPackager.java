package com.mobilesorcery.sdk.builder.java;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.CapabilityFragmentation;
import com.mobilesorcery.sdk.core.CapabilityState;
import com.mobilesorcery.sdk.core.CommandLineBuilder;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.ICapabilities;
import com.mobilesorcery.sdk.core.ICapability;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.PackageToolPackager;
import com.mobilesorcery.sdk.core.ParameterResolverException;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.ProfileDBManager;
import com.mobilesorcery.sdk.profiles.filter.DeviceCapabilitiesFilter;
import com.mobilesorcery.sdk.ui.DefaultMessageProvider;

public class JavaPackager extends PackageToolPackager {

	public final static String ID = "com.mobilesorcery.sdk.build.j2me.packager";

	public JavaPackager() {
		super();
	}

	@Override
	protected void addPlatformSpecifics(MoSyncProject project,
			IBuildVariant variant, CommandLineBuilder commandLine)
			throws Exception {
		KeystoreCertificateInfo keystoreCertInfo = null;

		boolean useProjectSpecific = PropertyUtil.getBoolean(project,
				PropertyInitializer.JAVAME_PROJECT_SPECIFIC_KEYS);
		boolean doSign = useProjectSpecific ? PropertyUtil.getBoolean(project,
				PropertyInitializer.JAVAME_DO_SIGN) : Activator.getDefault()
				.getPreferenceStore()
				.getBoolean(PropertyInitializer.JAVAME_DO_SIGN);

		if (doSign) {
			keystoreCertInfo = KeystoreCertificateInfo.loadOne(
					PropertyInitializer.JAVAME_KEYSTORE_CERT_INFOS,
					PropertyInitializer.JAVAME_PROJECT_SPECIFIC_KEYS, project,
					Activator.getDefault().getPreferenceStore());
			if (keystoreCertInfo == null
					|| !DefaultMessageProvider.isEmpty(keystoreCertInfo
							.validate(false))) {
				throw new CoreException(new Status(IStatus.OK,
						Activator.PLUGIN_ID, keystoreCertInfo.validate(false)
								.getMessage()));
			}
		}

		if (keystoreCertInfo != null) {
			String keystore = keystoreCertInfo.getKeystoreLocation();
			String alias = keystoreCertInfo.getAlias();
			String storepass = keystoreCertInfo.getKeystorePassword();
			String keypass = keystoreCertInfo.getKeyPassword();
			commandLine.flag("--javame-keystore").with(keystore);
			commandLine.flag("--javame-storepass", true).with(storepass);
			commandLine.flag("--javame-alias").with(alias);
			commandLine.flag("--javame-keypass", true).with(keypass);
		}
	}

	@Override
	public void createPackage(MoSyncProject project, IBuildSession session,
			IBuildVariant variant, IFileTreeDiff diff, IBuildResult buildResult)
			throws CoreException {
		super.createPackage(project, session, variant, diff, buildResult);
		FileInputStream mdStream = null;
		try {
			// We can remove this once we are sure we produce correct manifests.
			DefaultPackager intern = new DefaultPackager(project, variant);
			File mfFile = new File(
					intern.get(DefaultPackager.PACKAGE_OUTPUT_DIR),
					"META-INF/MANIFEST.MF");
			mdStream = new FileInputStream(mfFile);
			Manifest mf = new Manifest();
			mdStream = new FileInputStream(mfFile);
			mf.read(mdStream);
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Activator.PLUGIN_ID,
					"Internal error: could not create manifest", e));
		} finally {
			Util.safeClose(mdStream);
		}
	}

	@Override
	protected Map<String, List<File>> computeBuildResult(MoSyncProject project,
			IBuildVariant variant) throws ParameterResolverException {
		File jar = getProducedJar(project, variant);
		File jad = new File(Util.replaceExtension(jar.getAbsolutePath(), "jad"));
		HashMap<String, List<File>> result = new HashMap<String, List<File>>();
		result.put(IBuildResult.MAIN, Arrays.asList(jar));
		result.put(Activator.JAD, Arrays.asList(jad));
		return result;
	}

	protected File getProducedJar(MoSyncProject project, IBuildVariant variant) throws ParameterResolverException {
		DefaultPackager intern = new DefaultPackager(project, variant);
		return new File(intern.get(DefaultPackager.PACKAGE_OUTPUT_DIR),
				intern.get(DefaultPackager.APP_NAME) + ".jar");
	}

	@Override
	public String getShortDescription(MoSyncProject project, IProfile profile) {
		if (getClass() == JavaPackager.class
				&& profile.getProfileType() == MoSyncTool.DEFAULT_PROFILE_TYPE) {
			ArrayList<Object> result = new ArrayList<Object>();
			ICapabilities caps = ProfileDBManager.getInstance()
					.getCapabilities(profile);
			DeviceCapabilitiesFilter filter = DeviceCapabilitiesFilter.extractFilterFromProject(project);
			if (caps != null && filter != null) {
				ICapability iconSize = caps.getCapability("IconSize");
				if (iconSize != null) {
					result.add(iconSize.getValue());
				}
				Set<ICapability> deviceCapabilities = caps.listCapabilities();
				for (ICapability cap : deviceCapabilities) {
					String name = cap.getName();
					if (cap.getState() == CapabilityState.REQUIRES_PERMISSION
							|| cap.getState() == CapabilityState.REQUIRES_PRIVILEGED_PERMISSION
							&& cap.getFragmentation() == CapabilityFragmentation.BUILDTIME) {
						if (filter.getRequiredCapabilities().contains(name) ||
						filter.getOptionalCapabilities().contains(name)) {
							result.add(cap.getName());
						}
					}
				}
			}
			if (result.size() > 0) {
				return Util.join(result.toArray(), ", ");
			}
		}
		return super.getShortDescription(project, profile);
	}
}
