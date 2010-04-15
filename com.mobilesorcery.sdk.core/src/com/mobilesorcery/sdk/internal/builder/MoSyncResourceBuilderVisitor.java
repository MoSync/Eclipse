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
/**
 * 
 */
package com.mobilesorcery.sdk.internal.builder;

import java.util.ArrayList;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.internal.PipeTool;
import com.mobilesorcery.sdk.internal.dependencies.DependencyManager;
import com.mobilesorcery.sdk.internal.dependencies.IDependencyProvider;
import com.mobilesorcery.sdk.internal.dependencies.ResourceFileDependencyProvider;

public class MoSyncResourceBuilderVisitor extends IncrementalBuilderVisitor {
	private ArrayList<IResource> resourceFiles = new ArrayList<IResource>();
	private IPath outputFile;
	private PipeTool pipeTool;
	private String[] extraSwitches;

	private IDependencyProvider<IResource> dependencyProvider;

	public boolean visit(IResource resource) throws CoreException {
		super.visit(resource);

		if (isResourceFile(resource)) {
			resourceFiles.add(resource);
		}

		return true;
	}

	public static boolean isResourceFile(IResource resource) {
		if (resource.getType() == IResource.FILE) {
			IFile file = (IFile) resource;
			String name = file.getName();
			return (name.endsWith(".lst") && !name.startsWith("stabs."));
		}

		return false;
	}

	public String[] getResourceFiles() throws CoreException {
		// TODO: side-effect-less
		resourceFiles = new ArrayList<IResource>();
		project.accept(this);
		String[] result = new String[resourceFiles.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = resourceFiles.get(i).getLocation().toOSString();
		}
		return result;
	}

	public void setOutputFile(IPath outputFile) {
		this.outputFile = outputFile;
	}

	public void setPipeTool(PipeTool pipeTool) {
		this.pipeTool = pipeTool;
	}

	public void incrementalCompile(IProgressMonitor monitor, DependencyManager<IResource> dependencyManager) throws CoreException {
		Set<IResource> recompileThese = computeResourcesToRebuild(dependencyManager);
		if (!recompileThese.isEmpty()) {
			String[] extraResourceSwitches = PropertyUtil.getStrings(getBuildProperties(), MoSyncBuilder.EXTRA_RES_SWITCHES);
			pipeTool.setExtraSwitches(extraResourceSwitches);
			pipeTool.setMode(PipeTool.BUILD_RESOURCES_MODE);
			pipeTool.setInputFiles(getResourceFiles());
			pipeTool.setOutputFile(outputFile);
			pipeTool.run();
			
			// Explicitly add dependencies for the pipetool output file -- TODO:
			// outputfile must equal getresourceoutput; remove one.
			dependencyManager.setDependencies(ResourceFileDependencyProvider.getResourceOutput(project), dependencyProvider);
			dependencyManager.setDependencies(resourceFiles, dependencyProvider);
		}
	}

	public void setDependencyProvider(IDependencyProvider<IResource> dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
	}

	public boolean isBuildable(IResource resource) {
		return isResourceFile(resource);
	}

	protected String getName() {
		return "Resource Compiler";
	}

}