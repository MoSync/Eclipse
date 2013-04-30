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
import com.mobilesorcery.sdk.core.IPackager;
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
import com.mobilesorcery.sdk.profiles.IProfile;

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
		
		IProfile profile = variant.getProfile();
		IPackager packager = profile.getPackager();
		
		packager.buildNative(project, session, variant);
		
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
