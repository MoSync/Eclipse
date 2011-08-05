package com.mobilesorcery.sdk.core.build;

import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildState;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.core.IPackager;
import com.mobilesorcery.sdk.core.IProcessConsole;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.LineReader.ILineHandler;
import com.mobilesorcery.sdk.internal.PipeTool;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.Profile;

public class PackBuildStep extends AbstractBuildStep {

	public static class Factory extends AbstractBuildStepFactory {

		@Override
		public IBuildStep create() {
			return new PackBuildStep();
		}

		@Override
		public String getId() {
			return ID;
		}
		
		@Override
		public String getName() {
			return "Package";
		}
	}

	public static final String ID = "pack";

	public PackBuildStep() {
		setId(ID);
		setName("Pack");
	}
	
	@Override
	public int incrementalBuild(MoSyncProject mosyncProject, IBuildSession session,
			IBuildVariant variant, IFileTreeDiff diff,
			IBuildResult buildResult, IProgressMonitor monitor) throws Exception {
		IProcessConsole console = getConsole();
		IPropertyOwner buildProperties = getBuildProperties();
		IProfile targetProfile = variant.getProfile();

        monitor.setTaskName(MessageFormat.format("Packaging for {0}", targetProfile));

        IPackager packager = targetProfile.getPackager();
        packager.setParameter(MoSyncBuilder.USE_DEBUG_RUNTIME_LIBS, Boolean.toString(PropertyUtil
                .getBoolean(buildProperties, MoSyncBuilder.USE_DEBUG_RUNTIME_LIBS)));
        packager.createPackage(mosyncProject, variant, buildResult);

        if (buildResult.getBuildResult() == null || !buildResult.getBuildResult().exists()) {
            throw new IOException(MessageFormat.format("Failed to create package for {0} (platform: {1})", targetProfile, Profile.getAbbreviatedPlatform(targetProfile)));
        } else {
            console.addMessage(MessageFormat.format("Created package: {0} (platform: {1})", buildResult.getBuildResult(), Profile.getAbbreviatedPlatform(targetProfile)));
        }
        
        return CONTINUE;
	}

	@Override
	public boolean shouldBuild(MoSyncProject project, IBuildSession session, IBuildResult buildResult) {
		return super.shouldBuild(project, session, buildResult) && !MoSyncBuilder.isLib(project);
	}
	
	@Override
	public boolean shouldAdd(IBuildSession session) {
		return session.doPack();
	}
	
	@Override
	public String[] getDependees() {
		return new String[] { LinkBuildStep.ID };
	}
}
