package com.mobilesorcery.sdk.extensionsupport;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.core.BuildVariant;
import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncExtensionManager;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.PackageToolPackager;
import com.mobilesorcery.sdk.core.ProfileManager;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.build.AbstractBuildStep;
import com.mobilesorcery.sdk.core.build.AbstractBuildStepFactory;
import com.mobilesorcery.sdk.core.build.BuildSequence;
import com.mobilesorcery.sdk.core.build.IBuildStep;
import com.mobilesorcery.sdk.core.build.NativeLibBuildStep;
import com.mobilesorcery.sdk.internal.builder.MoSyncBuilderVisitor;
import com.mobilesorcery.sdk.profiles.IVendor;
import com.mobilesorcery.sdk.profiles.filter.DeviceCapabilitiesFilter;

public class ExtensionSupportBuildStep extends AbstractBuildStep {

	public static final String IDL_PHASE = "idl";
	public static final String PACK_PHASE = "pack";

	public static class Factory extends AbstractBuildStepFactory {

		private boolean shouldUpdateInstallation = true;
		private boolean generateStubs = false;
		
		private String phase = IDL_PHASE;

		@Override
		public IBuildStep create() {
			return new ExtensionSupportBuildStep(this);
		}

		@Override
		public String getId() {
			return ExtensionSupportBuildStepExtension.ID;
		}

		@Override
		public String getName() {
			return MessageFormat.format("Native Extensions ({0})", phase);
		}

		public void setPhase(String phase) {
			this.phase = phase;
		}

		public String getPhase() {
			return phase;
		}

		public boolean shouldUpdateInstallation() {
			return shouldUpdateInstallation;
		}

		public void shouldUpdateInstallation(boolean shouldUpdateInstallation) {
			this.shouldUpdateInstallation = shouldUpdateInstallation;
		}
		
		public boolean shouldGenerateStubs() {
			return generateStubs;
		}
		
		public void shouldGenerateStubs(boolean generateStubs) {
			this.generateStubs = generateStubs;
		}

		public String getPlatformBundleLocation(String platform) {
			return "%current-project%/" + platform;
		}

		@Override
		public void load(IMemento memento) {
			phase = memento.getString("phase");
			Object shouldUpdateInstallationBool = memento
					.getBoolean("should.update");
			Object shouldGenerateStubsBool = memento
					.getBoolean("generate.stubs");
			shouldUpdateInstallation = PACK_PHASE.equals(phase)
					&& shouldUpdateInstallationBool != Boolean.FALSE;
			generateStubs = IDL_PHASE.equals(phase) 
					&& shouldGenerateStubsBool == Boolean.TRUE;
		}

		@Override
		public void store(IMemento memento) {
			memento.putString("phase", phase);
			if (PACK_PHASE.equals(phase)) {
				memento.putBoolean("should.update", shouldUpdateInstallation);
			}
			if (IDL_PHASE.equals(phase)) {
				memento.putBoolean("generate.stubs", generateStubs);
			}
		}

	}

	private Factory prototype;
	private String phase;

	public ExtensionSupportBuildStep(Factory prototype) {
		this.prototype = prototype;
		this.phase = prototype.phase;
		setName(prototype.getName());
	}

	@Override
	public int incrementalBuild(MoSyncProject project, IBuildSession session,
			IBuildVariant variant, IFileTreeDiff diff, IBuildResult result,
			IProgressMonitor monitor) throws Exception {
		// First make sure the project is ok...
		if (!MoSyncBuilder.isExtension(project)) {
			throw new IllegalStateException(
					"This is not an extension project.");
		}

		// Preparations:
		IPath libOutput = MoSyncBuilder.computeLibraryOutput(
				project,
				MoSyncBuilder.getPropertyOwner(project,
						variant.getConfigurationId()));
		String extensionName = Util.getNameWithoutExtension(libOutput.toFile());
		IPath extensionOutput = libOutput.removeLastSegments(1).append(
				Util.replaceExtension(libOutput.lastSegment(), "ext"));
		File unzippedExtensionOutput = libOutput.removeLastSegments(1)
				.append(extensionName).toFile();
		unzippedExtensionOutput.mkdirs();

		// 1. Parse the extension manifest file

		// 2. Create a library + header files out of the idl file (used by all
		// platforms)
		// as well as platform specific stuff (such as android assets, etc).
		if (IDL_PHASE.equals(phase)) {
			ExtensionCompiler.getDefault().compile(project, shouldGenerateStubs());
		} else {
			// 3. Gather all platform libs
			ProfileManager prMgr = MoSyncTool.getDefault().getProfileManager(
					MoSyncTool.DEFAULT_PROFILE_TYPE);
			IVendor[] platforms = prMgr.getVendors();

			HashSet<String> supportedPlatforms =
					new HashSet<String>(Arrays.asList(PropertyUtil.getStrings(
							project, ExtensionSupportPlugin.SUPPORTED_PLATFORMS_PROP)));
			
			for (IVendor platform : platforms) {
				if (supportedPlatforms.contains(platform.getName())) {
					String platformName = platform.getName();
					String bundleLocation = prototype
							.getPlatformBundleLocation(platformName);
					// TODO: Error checking, etc.
					File bundleLocationFile = new File(Util.replace(bundleLocation,
							getParameterResolver()));
					File bundleOutput = new File(unzippedExtensionOutput,
							platformName);
					if (!bundleLocationFile.exists()) {
						throw new IllegalArgumentException("No platform specific code for " + platform);
					} else {
						bundleOutput.mkdirs();
						Util.copy(monitor, bundleLocationFile, bundleOutput, null);
					}
				}
			}

			// 4. Copy the manifest file
			Util.copyFile(monitor, project.getWrappedProject().getLocation()
					.append("extension.mf").toFile(), unzippedExtensionOutput);

			// 5. Zip it!
			File libDirectory = new File(unzippedExtensionOutput, "lib");
			libDirectory.mkdirs();
			File incDirectory = new File(unzippedExtensionOutput, "inc");
			incDirectory.mkdirs();
			// TODO: Header file should be generated in another way!
			Util.copyDir(
					monitor,
					project.getWrappedProject().getLocation().toFile(),
					incDirectory,
					Util.getExtensionFilter(MoSyncBuilderVisitor.C_HEADER_FILE_EXTS),
					1);

			Util.copyFile(monitor, libOutput.toFile(), libDirectory);
			
			// If applicable, JavaScript should also go in there.
			if (PropertyUtil.getBoolean(project, ExtensionSupportPlugin.GENERATE_JS_PROP)) {
				String jsLibLocation = Util.replace(prototype.getPlatformBundleLocation("js"), getParameterResolver());
				Util.copyDir(monitor, new File(jsLibLocation), new File(libDirectory, "js"), Util.getExtensionFilter("js"));
			}

			// 5. Build native code.
			BuildSequence seq = BuildSequence.getCached(project);
			List<NativeLibBuildStep.Factory> nativeBuild = seq.getBuildStepFactories(NativeLibBuildStep.Factory.class);
			if (!nativeBuild.isEmpty()) {
				NativeLibBuildStep nativeBuildStep = (NativeLibBuildStep) nativeBuild.get(0).create();
				ArrayList<IBuildVariant> variants = new ArrayList<IBuildVariant>();
				ArrayList<String> debugCfgs = new ArrayList<String>(project.getBuildConfigurationsOfType(IBuildConfiguration.DEBUG_TYPE));
				ArrayList<String> releaseCfgs = new ArrayList<String>(project.getBuildConfigurationsOfType(IBuildConfiguration.RELEASE_TYPE));
				if (debugCfgs.size() != 1 || releaseCfgs.size() != 1) {
					throw new CoreException(new Status(IStatus.ERROR, ExtensionSupportPlugin.PLUGIN_ID,
							"Exactly one debug and one release configuration required."));
				}
				//TODO: One for each platform!!
				BuildVariant dbgVariant = new BuildVariant(variant);
				dbgVariant.setConfigurationId(debugCfgs.get(0));
				BuildVariant relVariant = new BuildVariant(variant);
				relVariant.setConfigurationId(releaseCfgs.get(0));
				variants.add(relVariant);
				variants.add(dbgVariant);
				
				for (IBuildVariant variantToBuild : variants) {
				nativeBuildStep.build(project, session, variantToBuild, result);
					List<File> libFiles = result.getBuildResult().get(PackageToolPackager.NATIVE_LIBS);
					for (File libFile : libFiles) {
						// Mini-hack to get the lib file name + parent dir name.
						IPath lastTwo = new Path(libFile.getAbsolutePath());
						lastTwo = lastTwo.removeFirstSegments(lastTwo.segmentCount() - 2);
						File libDst = new File(libDirectory, lastTwo.toOSString());
						Util.copy(monitor, libFile, libDst, null);
					}
				}
			}
			
			// 6. Zip it all!
			Util.zip(unzippedExtensionOutput, extensionOutput.toFile());
			
			// 7. If this setting is there, also update the extension library in
			// the current mosync installation.
			if (shouldUpdateInstallation()) {
				MoSyncExtensionManager.getDefault().install(
						unzippedExtensionOutput, true);
			}

			result.setBuildResult(IBuildResult.MAIN, extensionOutput.toFile());
		}
		// MOVE ALL PLATFORM SPEC STUFF LATER ON!
		return CONTINUE;
	}

	private boolean shouldUpdateInstallation() {
		return prototype.shouldUpdateInstallation();
	}
	
	private boolean shouldGenerateStubs() {
		return prototype.shouldGenerateStubs();
	}

}
