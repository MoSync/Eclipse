package com.mobilesorcery.sdk.core.build;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.mobilesorcery.sdk.core.CommandLineBuilder;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.ParameterResolver;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.internal.builder.IncrementalBuilderVisitor;
import com.mobilesorcery.sdk.internal.builder.MoSyncBuilderVisitor;

// TODO: Move this -- this is quite android specific as of now. Also,
// we should use workfiles later on. So no bug reports on this one, please :)
public class NativeLibBuildStep extends AbstractBuildStep {

	public static class Factory extends AbstractBuildStepFactory {

		@Override
		public IBuildStep create() {
			return new NativeLibBuildStep(this);
		}

		@Override
		public String getId() {
			return ID;
		}

		@Override
		public String getName() {
			return "Build using native tools";
		}
	}
	
	public static final String ID = "native";

	public NativeLibBuildStep(Factory factory) {
		setId(factory.getId());
		setName(factory.getName());
	}

	@Override
	public int incrementalBuild(MoSyncProject project, IBuildSession session,
			IBuildVariant variant, IFileTreeDiff diff, IBuildResult result,
			IProgressMonitor monitor) throws Exception {
		IPropertyOwner properties = MoSyncBuilder.getPropertyOwner(project, variant.getConfigurationId());
		if (!MoSyncBuilder.OUTPUT_TYPE_NATIVE_COMPILE.equals(properties.getProperty(MoSyncBuilder.OUTPUT_TYPE))) {
			getConsole().addMessage("Project does not use native compilation");
			return CONTINUE;
		}
		
		CommandLineBuilder commandLine = new CommandLineBuilder(MoSyncTool.getDefault().getBinary("nbuild").toOSString());
		commandLine.flag("--platform").with(variant.getProfile().getVendor().getName());
		//TODO: Spaces and special chars?
		commandLine.flag("--name").with(project.getName());
		commandLine.flag("--project").with(project.getWrappedProject().getLocation().toFile());
		commandLine.flag("--dst").with(MoSyncBuilder.getPackageOutputPath(project.getWrappedProject(), variant).removeLastSegments(1).toOSString());
		commandLine.flag("--config").with(variant.getConfigurationId());
		boolean isDebug = PropertyUtil.getBoolean(properties, MoSyncBuilder.USE_DEBUG_RUNTIME_LIBS);
		String libVariant = isDebug ? "debug" : "release";
		commandLine.flag("--lib-variant").with(libVariant);
		String[] modules = PropertyUtil.getStrings(project, MoSyncBuilder.EXTENSIONS);
		if (modules.length > 0) {
			commandLine.flag("--modules").with(Util.join(modules, ","));
		}
	
		String compilerSwitches = MoSyncBuilder.getExtraCompilerSwitches(project, variant);
		// TODO: Platform independent!?
		commandLine.flag("--compiler-switches").with(compilerSwitches);
		
		// This one makes sure that exclusions, wild cards, etc work as before!
		IncrementalBuilderVisitor visitor = new IncrementalBuilderVisitor() {
			public boolean doesAffectBuild(IResource resource) {
				return super.doesAffectBuild(resource) && MoSyncBuilderVisitor.hasExtension(resource, MoSyncBuilderVisitor.C_SOURCE_FILE_EXTS);
			}
		};
		visitor.setProject(project.getWrappedProject());
		visitor.setDiff(null);
		Set<IResource> sourceFiles = visitor.computeResourcesToRebuild(null);
		ArrayList<String> listOfRelativeFiles = new ArrayList<String>();
		for (IResource sourceFile : sourceFiles) {
			listOfRelativeFiles.add(sourceFile.getProjectRelativePath().toOSString());
			commandLine.flag("-S" + sourceFile.getProjectRelativePath().toOSString());
		}
		
		commandLine.flag("-I" + MoSyncBuilder.getOutputPath(project.getWrappedProject(), variant).toOSString());
		//commandLine.flag("--source-files").with(Util.join(listOfRelativeFiles.toArray(), " "));
		
		commandLine.flag("--verbose");

		// Android specific stuff
		IPreferenceStore androidPrefs = new ScopedPreferenceStore(InstanceScope.INSTANCE, "com.mobilesorcery.sdk.builder.android");
		String ndkLocation = androidPrefs.getString("android.ndk");
		int platformVersion = androidPrefs.getInt("android.platform.version");
		if (Util.isEmpty(ndkLocation)) {
			throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, "Missing NDK location"));
		}
		if (platformVersion < 1) {
			throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, "Missing NDK version"));
		}
		commandLine.flag("--android-ndkbuild-cmd").with(new File(ndkLocation, "ndk-build"));
		commandLine.flag("--android-version").with(Integer.toString(platformVersion));
		
		DefaultPackager internal = new DefaultPackager(project, variant);
		internal.runCommandLine(commandLine.asArray(),
				commandLine.toHiddenString());
		return IBuildStep.CONTINUE;
	}
}
