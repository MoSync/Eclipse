package com.mobilesorcery.sdk.core.build;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
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
import com.mobilesorcery.sdk.core.IProcessConsole;
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
		boolean setNative = MoSyncBuilder.OUTPUT_TYPE_NATIVE_COMPILE.equals(project.getProperty(MoSyncBuilder.OUTPUT_TYPE));
		if (setNative && project.getProfileManagerType() != MoSyncTool.DEFAULT_PROFILE_TYPE) {
			throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID,
					"Native compilation is only supported for projects using the platform profile database."));
		}
		if (!isOutputType(project, variant, MoSyncBuilder.OUTPUT_TYPE_NATIVE_COMPILE)) {
			getConsole().addMessage("Project does not use native compilation");
			if (setNative) {
				getConsole().addMessage(IProcessConsole.ERR, "The platform does not support native compilation");
			}
			return CONTINUE;
		}

		assertConfigId(variant);
		
		CommandLineBuilder commandLine = new CommandLineBuilder(MoSyncTool.getDefault().getBinary("nbuild").toOSString());
		commandLine.flag("--platform").with(variant.getProfile().getVendor().getName());
		//TODO: Spaces and special chars?
		commandLine.flag("--name").with(project.getName());
		commandLine.flag("--project").with(project.getWrappedProject().getLocation().toFile());
		IPath dst = MoSyncBuilder.getPackageOutputPath(project.getWrappedProject(), variant).removeLastSegments(1);
		commandLine.flag("--dst").with(dst.toOSString());
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
		
        List<IPath> includePaths = new ArrayList<IPath>();
        // Todo: how to handle additional includes
        includePaths.addAll(Arrays.asList(MoSyncBuilder.getBaseIncludePaths(project, variant)));
        //includePaths.addAll(Arrays.asList(MoSyncTool.getDefault().getMoSyncDefaultIncludes()));
        //includePaths.add(MoSyncBuilder.getOutputPath(project.getWrappedProject(), variant));
		String[] includes = MoSyncBuilderVisitor.assembleIncludeString(includePaths.toArray(new IPath[0]));
		for (String include : includes) {
			commandLine.flag(include);
		}
		
		commandLine.flag("--verbose");

		// Android specific stuff
		if ("android".equalsIgnoreCase(variant.getProfile().getVendor().getName())) {
			IPreferenceStore androidPrefs = new ScopedPreferenceStore(InstanceScope.INSTANCE, "com.mobilesorcery.sdk.builder.android");
			String ndkLocation = androidPrefs.getString("android.ndk");
			int platformVersion = androidPrefs.getInt("android.platform.version");
			if (Util.isEmpty(ndkLocation)) {
				throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, "Missing NDK location"));
			}
			if (platformVersion < 1) {
				throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, "Missing NDK version"));
			}
			commandLine.flag("--android-ndk-location").with(new File(ndkLocation));
			commandLine.flag("--android-version").with(Integer.toString(platformVersion));
			commandLine.flag("--android-build-dir").with(dst.append(new Path("temp")).toOSString());
		}
		
		DefaultPackager internal = new DefaultPackager(project, variant);
		if (internal.runCommandLine(commandLine.asArray(),
				commandLine.toHiddenString()) != 0) {
			throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, "Build failed."));
		}
		return IBuildStep.CONTINUE;
	}
	
	private static void assertConfigId(IBuildVariant variant) throws CoreException {
		String validatedConfig = validateConfigId(variant);
		if (!Util.isEmpty(validatedConfig)) {
			throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID,
				validatedConfig));
		}
	}
	
	public static String validateConfigId(IBuildVariant variant) {
		String configId = variant.getConfigurationId();
		for (int i = 0; i < configId.length(); i++) {
			char ch = configId.charAt(i);
			if (!Character.isLetter(ch) && !Character.isDigit(ch) &&
				ch != '-' && ch !=  '_') {
				return MessageFormat.format(
						"Invalid configuration name: {0}. Only letters, digits, hyphen and underscored allowed.",
						configId);
			}
		}
		return null;
	}
}
