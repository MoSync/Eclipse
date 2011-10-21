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
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.mobilesorcery.sdk.internal.dependencies.DependencyManager.Delta;

public class BuildResult implements IBuildResult {

    private File buildResult;
    private final ArrayList<String> errors = new ArrayList<String>();
    private final IProject project;
	private boolean success;
	private IBuildVariant variant;
    private long timestamp;
	private Delta<IResource> dependencyDelta;
	private final HashMap<String, File> intermediateBuildResults = new HashMap<String, File>();

    public BuildResult(IProject project) {
        this.project = project;
    }

    @Override
	public void addError(String errorMsg) {
        this.errors.add(errorMsg);
    }

    @Override
	public List<String> getErrors() {
    	return errors;
    }

    @Override
	public File getBuildResult() {
        return buildResult;
    }

    @Override
	public void setBuildResult(File buildResult) {
        this.buildResult = buildResult;
    }

	@Override
	public boolean success() {
		return success && errors.isEmpty();
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public IProject getProject() {
		return project;
	}

	@Override
	public IBuildVariant getVariant() {
		return variant;
	}

	public void setVariant(IBuildVariant variant) {
		this.variant = variant;
	}

    @Override
	public long getTimestamp() {
        return timestamp;
    }

    @Override
	public void setTimestamp(long utc) {
        this.timestamp = utc;
    }

	public void setDependencyDelta(Delta<IResource> dependencyDelta) {
		this.dependencyDelta = dependencyDelta;
	}

	@Override
	public Delta<IResource> getDependencyDelta() {
		return dependencyDelta;
	}

	@Override
	public File getIntermediateBuildResult(String buildStepId) {
		return intermediateBuildResults.get(buildStepId);
	}

	@Override
	public void setIntermediateBuildResult(String buildStepId, File file) {
		intermediateBuildResults.put(buildStepId, file);
	}
}
