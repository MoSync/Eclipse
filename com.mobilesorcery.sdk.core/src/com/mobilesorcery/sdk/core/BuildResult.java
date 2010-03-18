/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.core;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;

import com.mobilesorcery.sdk.profiles.IProfile;

public class BuildResult implements IBuildResult {

    private File buildResult;
    private ArrayList<String> errors = new ArrayList<String>();
    private IProject project;
	private boolean success;
	private IProfile profile;

    public BuildResult(IProject project) {
        this.project = project;
    }

    /* (non-Javadoc)
     * @see com.mobilesorcery.sdk.core.api.IBuildRest#addError(java.lang.String)
     */
    public void addError(String errorMsg) {
        this.errors.add(errorMsg);
    }
    
    /* (non-Javadoc)
     * @see com.mobilesorcery.sdk.core.api.IBuildRest#getBuildResult()
     */
    public File getBuildResult() {
        return buildResult;
    }
    
    /* (non-Javadoc)
     * @see com.mobilesorcery.sdk.core.api.IBuildRest#setBuildResult(java.io.File)
     */
    public void setBuildResult(File buildResult) {
        this.buildResult = buildResult;
    }

	public boolean success() {
		return success && errors.isEmpty();
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}
    
	public IProject getProject() {
		return project;
	}

	/**
	 * @deprecated Move to other class
	 */
	public IProfile getProfile() {
		return profile;
	}
	
	/**
	 * @deprecated Move to other class
	 */
	public void setProfile(IProfile profile) {
		this.profile = profile;
	}
}
