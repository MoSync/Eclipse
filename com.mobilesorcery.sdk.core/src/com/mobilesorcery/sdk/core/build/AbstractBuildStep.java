/*  Copyright (C) 2013 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.mobilesorcery.sdk.core.build;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildState;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.core.IPackager;
import com.mobilesorcery.sdk.core.IProcessConsole;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.ParameterResolver;
import com.mobilesorcery.sdk.core.LineReader.ILineHandler;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.security.IApplicationPermissions;
import com.mobilesorcery.sdk.internal.PipeTool;
import com.mobilesorcery.sdk.internal.Binutils;
import com.mobilesorcery.sdk.internal.dependencies.DependencyManager;
import com.mobilesorcery.sdk.internal.dependencies.IDependencyProvider;
import com.mobilesorcery.sdk.profiles.IProfile;

public abstract class AbstractBuildStep implements IBuildStep {

	/**
	 * A constant that build steps may use to indicate to
	 * subsequent build steps that another set of permissions
	 * should be used for building.
	 * This should be added to a {@link IBuildSession}'s
	 * properties and must be of type {@link IApplicationPermissions}.
	 */
	public static final String MODIFIED_PERMISSIONS = CoreMoSyncPlugin.PLUGIN_ID + "mod.perm";

	private IProcessConsole console;
	private IPropertyOwner buildProperties;
	private IBuildState buildState;
	private PipeTool pipeTool;
	private Binutils binutils;
	private ILineHandler defaultLineHandler;
	private IDependencyProvider<IResource> dependencyProvider;
	private String id;
	private String name;
	private ParameterResolver resolver;
	private IFilter<IResource> resourceFilter;

	public String getId() {
		return id;
	}

	protected void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	protected void setName(String name) {
		this.name = name;
	}

	@Override
	public void initConsole(IProcessConsole console) {
		this.console = console;
	}

	protected IProcessConsole getConsole() {
		return console;
	}

	@Override
	public void initBuildProperties(IPropertyOwner buildProperties) {
		this.buildProperties = buildProperties;
	}

	protected IPropertyOwner getBuildProperties() {
		return buildProperties;
	}

	@Override
	public void initBuildState(IBuildState buildState) {
		this.buildState = buildState;
	}

	protected IBuildState getBuildState() {
		return buildState;
	}

	public void initPipeTool(PipeTool pipeTool) {
		this.pipeTool = pipeTool;
	}

	protected PipeTool getPipeTool() {
		return pipeTool;
	}

	public void initBinutils(Binutils b) {
		this.binutils = b;
	}

	protected Binutils getBinutils() {
		return binutils;
	}

	public void initDefaultLineHandler(ILineHandler defaultLineHandler) {
		this.defaultLineHandler = defaultLineHandler;
	}

	protected ILineHandler getDefaultLineHandler() {
		return defaultLineHandler;
	}

	public void initDependencyProvider(IDependencyProvider<IResource> dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
	}

	protected IDependencyProvider<IResource> getDependencyProvider() {
		return dependencyProvider;
	}

	@Override
	public void initResourceFilter(IFilter<IResource> resourceFilter) {
		this.resourceFilter = resourceFilter;
	}

	protected IFilter<IResource> getResourceFilter() {
		return resourceFilter;
	}

	public void initParameterResolver(ParameterResolver resolver) {
		this.resolver = resolver;
	}

	public ParameterResolver getParameterResolver() {
		return resolver;
	}

	/**
	 * The default implementation returns <code>true</code> if
	 * the current build has no errors from previous build steps
	 * and if this step should be added (as per {@link #shouldAdd(IBuildSession)})
	 */
	@Override
	public boolean shouldBuild(MoSyncProject project, IBuildSession session, IBuildResult buildResult) {
		return shouldAdd(session) && buildResult.getErrors().isEmpty();
	}

	@Override
	public boolean shouldAdd(IBuildSession session) {
		return true;
	}

	@Override
	public String[] getDependees() {
		return null;
	}

    protected Set<IProject> computeProjectDependencies(IProgressMonitor monitor, MoSyncProject mosyncProject, IBuildState buildState, IResource[] allAffectedResources) {
        IProject project = mosyncProject.getWrappedProject();
        monitor.setTaskName(MessageFormat.format("Computing project dependencies for {0}", project.getName()));
        DependencyManager<IProject> projectDependencies = CoreMoSyncPlugin.getDefault().getProjectDependencyManager(ResourcesPlugin.getWorkspace());
        projectDependencies.clearDependencies(project);
        HashSet<IProject> allProjectDependencies = new HashSet<IProject>();
        Set<IResource> dependencies = buildState.getDependencyManager().getDependenciesOf(Arrays.asList(allAffectedResources));
        for (IResource resourceDependency : dependencies) {
            if (resourceDependency.getType() != IResource.ROOT) {
                allProjectDependencies.add(resourceDependency.getProject());
            }
        }

        // No deps on self
        allProjectDependencies.remove(project);

        if (CoreMoSyncPlugin.getDefault().isDebugging()) {
            CoreMoSyncPlugin
                    .trace(MessageFormat.format("Computed project dependencies. Project {0} depends on {1}", project.getName(), allProjectDependencies));
        }
        return allProjectDependencies;
    }

    public String toString() {
    	return getId();
    }

    /**
     * Returns {@code true} if the <b>resolved</b> output type equals
     * a certain value.
     * @see {@link #getResolvedOutputType()}
     * @param project
     * @param variant
     * @param outputType
     * @return A boolean indicating whether this is, in fact, the requested
     * output type.
     */
    public boolean isOutputType(MoSyncProject project, IBuildVariant variant, String outputType) {
    	return Util.equals(outputType, getOutputType(project, variant));
    }

    /**
     * Returns the <b>resolved</b> output type to use. Does not necessarily correspond to
     * the project property ({@link MoSyncBuilder#OUTPUT_TYPE()}, since not all platforms
     * support them.
     * @return
     */
    protected String getOutputType(MoSyncProject project, IBuildVariant variant) {
    	String rawOutputType = project.getProperty(MoSyncBuilder.OUTPUT_TYPE);
    	IProfile profile = variant.getProfile();
    	IPackager platform = profile.getPackager();
    	if (platform == null) {
    		return rawOutputType;
    	} else {
    		return platform.getOutputType(project);
    	}
    }

}
