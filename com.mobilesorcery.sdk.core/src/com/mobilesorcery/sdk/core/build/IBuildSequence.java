package com.mobilesorcery.sdk.core.build;

import java.util.List;

import com.mobilesorcery.sdk.core.IBuildSession;

public interface IBuildSequence {

    /**
     * Returns the list of build steps for a session
     * @return
     */
	public List<IBuildStep> getBuildSteps(IBuildSession session);
}
