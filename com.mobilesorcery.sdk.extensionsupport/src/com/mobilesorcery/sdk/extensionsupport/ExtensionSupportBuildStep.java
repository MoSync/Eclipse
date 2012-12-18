package com.mobilesorcery.sdk.extensionsupport;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncExtensionManager;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.ProfileManager;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.build.AbstractBuildStep;
import com.mobilesorcery.sdk.core.build.AbstractBuildStepFactory;
import com.mobilesorcery.sdk.core.build.IBuildStep;
import com.mobilesorcery.sdk.internal.builder.MoSyncBuilderVisitor;
import com.mobilesorcery.sdk.profiles.IVendor;

public class ExtensionSupportBuildStep extends AbstractBuildStep {

	public static final String IDL_PHASE = "idl";
	public static final String PACK_PHASE = "pack";

	public static class Factory extends AbstractBuildStepFactory {

		private boolean shouldUpdateInstallation = true;
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

		public boolean shouldUpdateInstallation() {
			return shouldUpdateInstallation;
		}

		public String getPlatformBundleLocation(String platform) {
			return "%current-project%/" + platform;
		}

		@Override
		public void load(IMemento memento) {
			phase = memento.getString("phase");
		}

		@Override
		public void store(IMemento memento) {
			memento.putString("phase", phase);
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
		if (!MoSyncBuilder.isLib(project)) {
			throw new IllegalStateException(
					"Extension projects must be set to library build.");
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
			ExtensionCompiler.getDefault().compile(project);
		} else {

			// 3. Gather all platform libs
			ProfileManager prMgr = MoSyncTool.getDefault().getProfileManager(
					MoSyncTool.DEFAULT_PROFILE_TYPE);
			IVendor[] platforms = prMgr.getVendors();

			for (IVendor platform : platforms) {
				String platformName = platform.getName();
				String bundleLocation = prototype
						.getPlatformBundleLocation(platformName);
				// TODO: Error checking, etc.
				File bundleLocationFile = new File(Util.replace(bundleLocation,
						getParameterResolver()));
				File bundleOutput = new File(unzippedExtensionOutput,
						platformName);
				if (bundleLocationFile.exists()) {
					bundleOutput.mkdirs();
					Util.copy(monitor, bundleLocationFile, bundleOutput, null);
				}
			}

			// 4. Copy the manifest file
			Util.copyFile(monitor, project.getWrappedProject().getLocation().append("extension.mf").toFile(), unzippedExtensionOutput);
			
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
			Util.zip(unzippedExtensionOutput, extensionOutput.toFile());

			// 5. If this setting is there, also update the extension library in
			// the current mosync installation.
			if (shouldUpdateInstallation()) {
				MoSyncExtensionManager.getDefault().install(unzippedExtensionOutput, true);
			}

			result.setBuildResult(IBuildResult.MAIN, extensionOutput.toFile());
		}
		// MOVE ALL PLATFORM SPEC STUFF LATER ON!
		return CONTINUE;
	}

	private boolean shouldUpdateInstallation() {
		return prototype.shouldUpdateInstallation();
	}

}
