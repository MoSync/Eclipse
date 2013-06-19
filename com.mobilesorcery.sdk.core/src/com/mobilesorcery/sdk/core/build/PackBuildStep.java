package com.mobilesorcery.sdk.core.build;

import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.core.BuildResult;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
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
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.LineReader.ILineHandler;
import com.mobilesorcery.sdk.core.Util;
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
		IProfile targetProfile = variant.getProfile();

		// Special hack for MoRe (I don't think this is how it should work though.)
		if (targetProfile.isEmulator() && buildResult.getBuildResult().get(IBuildResult.MAIN) == null) {
			buildResult.setBuildResult(IBuildResult.MAIN, buildResult.getIntermediateBuildResult(LinkBuildStep.ID));
		} else {
	        monitor.setTaskName(MessageFormat.format("Packaging for {0}", targetProfile));
	        IPackager packager = targetProfile.getPackager();
	        packager.createPackage(mosyncProject, session, variant, diff, buildResult);
		}

        if (buildResult.getBuildResult() == null || !BuildResult.exists(buildResult.getBuildResult())) {
        	if (buildResult.getBuildResult() != null) {
        		CoreMoSyncPlugin.getDefault().getLog().log(new Status(IStatus.WARNING, CoreMoSyncPlugin.PLUGIN_ID,
        			MessageFormat.format("Expected build result at {0}, but did not find it", buildResult.getBuildResult())));
        	}
        	throw new IOException(MessageFormat.format("Failed to create package for {0}", targetProfile));
        } else {
            console.addMessage(MessageFormat.format("Created package for profile {0}:\n\t{1}",
            		 MoSyncTool.toString(targetProfile),
            		 Util.join(buildResult.getBuildResult().values().toArray(), "\n\t")));
        }

        return CONTINUE;
	}

	@Override
	public boolean shouldBuild(MoSyncProject project, IBuildSession session, IBuildResult buildResult) {
		return super.shouldBuild(project, session, buildResult) && !MoSyncBuilder.isLib(project) && !MoSyncBuilder.isExtension(project);
	}

	@Override
	public boolean shouldAdd(IBuildSession session) {
		return session.doPack();
	}

	@Override
	public String[] getDependees() {
		return new String[] { };
	}
}
