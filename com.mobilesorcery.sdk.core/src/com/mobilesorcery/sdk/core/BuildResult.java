package com.mobilesorcery.sdk.core;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;

public class BuildResult implements IBuildResult {

    private File buildResult;
    private ArrayList<String> errors = new ArrayList<String>();
    private IProject project;
	private boolean success;

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
    
}
