package com.mobilesorcery.sdk.internal.builder;

import java.util.HashMap;

import com.mobilesorcery.sdk.core.BuildResult;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildResultManager;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.profiles.IProfile;

public class BuildResultManager implements IBuildResultManager {

	private HashMap<IProfile, BuildResult> results = new HashMap<IProfile, BuildResult>();
	private MoSyncProject project;
	
	public BuildResultManager(MoSyncProject project) {
		this.project = project;
	}
	
	public IBuildResult getBuildResult(IProfile profile) {
		return results.get(profile);
	}

	public BuildResult clearBuildResult(IProfile profile) {
		BuildResult newResult = new BuildResult(project.getWrappedProject());
		newResult.setProfile(profile);
		results.put(profile, newResult);
		return newResult;
	}

}
