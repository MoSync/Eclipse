package com.mobilesorcery.sdk.core;

import com.mobilesorcery.sdk.profiles.IProfile;

public interface IBuildResultManager {

	/**
	 * Returns a new build result for <code>profile</code>.
	 * Calling getProfile() on the result will return the
	 * profile passed in as argument.
	 * @param profile
	 * @return
	 */
	BuildResult clearBuildResult(IProfile profile);
	
	IBuildResult getBuildResult(IProfile profile);

}
