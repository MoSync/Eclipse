package com.mobilesorcery.sdk.internal.builder;

import java.util.HashMap;

import com.mobilesorcery.sdk.core.BuildResult;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildResultManager;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.profiles.IProfile;

public class BuildResultManager implements IBuildResultManager {

	private HashMap<IBuildVariant, BuildResult> results = new HashMap<IBuildVariant, BuildResult>();
	private MoSyncProject project;
	
	public BuildResultManager(MoSyncProject project) {
		this.project = project;
	}
	
	public IBuildResult getBuildResult(IBuildVariant variant) {
		return results.get(variant);
	}

	public BuildResult clearBuildResult(IBuildVariant variant) {
		BuildResult newResult = new BuildResult(project.getWrappedProject());
		newResult.setVariant(variant);
		results.put(variant, newResult);
		return newResult;
	}

}
