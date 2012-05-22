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
package com.mobilesorcery.sdk.internal.builder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.core.IProcessConsole;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.ParameterResolver;
import com.mobilesorcery.sdk.core.ParameterResolverException;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.internal.dependencies.DependencyManager;
import com.mobilesorcery.sdk.internal.dependencies.IDependencyProvider;

/**
 * <p>
 * A base class for incremental builders.
 * </p>
 * <p>
 * It has support for aggregating changed, added, and deleted resources.
 * </p>
 *
 * @author Mattias Bybro
 *
 */
public abstract class IncrementalBuilderVisitor implements IResourceVisitor {

	protected List<IResource> changedOrAddedResources = new ArrayList<IResource>();
	protected List<IResource> deletedResources = new ArrayList<IResource>();
	protected IProject project;
	protected IProcessConsole console;
	private IFilter<IResource> resourceFilter;
    private IBuildVariant variant;
	private IDependencyProvider<IResource> dependencyProvider;
	private ParameterResolver resolver;

	@Override
	public boolean visit(IResource resource) throws CoreException {
		boolean isDerived = resource.isDerived();
		if (!isDerived && doesAffectBuild(resource)) {
			changedOrAddedResources.add(resource);
		}
		return !isDerived;
	}

	public void addChangedOrAddedResources(IResource[] changedOrAddedResources) {
		for (int i = 0; i < changedOrAddedResources.length; i++) {
			IResource resource = changedOrAddedResources[i];
			addChangedOrAddedResource(resource);
		}
	}

	public void addChangedOrAddedResource(IResource changedOrAddedResource) {
        if (doesAffectBuild(changedOrAddedResource)) {
            this.changedOrAddedResources.add(changedOrAddedResource);
        }
	}

    public void addDeletedResources(IResource[] deletedResources) {
        for (int i = 0; i < deletedResources.length; i++) {
            IResource resource = deletedResources[i];
            addDeletedResource(resource);
        }
    }

    public void addDeletedResource(IResource deletedResource) {
        if (doesAffectBuild(deletedResource)) {
            this.deletedResources.add(deletedResource);
        }
    }

	public void addChangedOrAddedResources(
			IResourceDelta[] changedOrAddedResourceDeltas) {
		IResource[] changedOrAddedResources = new IResource[changedOrAddedResourceDeltas.length];
		for (int i = 0; i < changedOrAddedResourceDeltas.length; i++) {
			changedOrAddedResources[i] = changedOrAddedResourceDeltas[i]
					.getResource();
			addChangedOrAddedResources(changedOrAddedResourceDeltas[i]
					.getAffectedChildren(IResourceDelta.ADDED
							| IResourceDelta.CHANGED));
		}

		addChangedOrAddedResources(changedOrAddedResources);
	}

	public IResource[] getChangedOrAddedResources() {
		return changedOrAddedResources.toArray(new IResource[0]);
	}

	public void addDeletedResources(IResourceDelta[] deletedResourceDeltas) {
		IResource[] deletedResources = new IResource[deletedResourceDeltas.length];
		for (int i = 0; i < deletedResourceDeltas.length; i++) {
			deletedResources[i] = deletedResourceDeltas[i].getResource();
			addDeletedResources(deletedResourceDeltas[i]
					.getAffectedChildren(IResourceDelta.REMOVED));
		}

		addDeletedResources(deletedResources);
	}

	public IResource[] getDeletedResources() {
		return deletedResources.toArray(new IResource[0]);
	}

	public IResource[] getAllAffectedResources() {
		ArrayList<IResource> result = new ArrayList<IResource>(deletedResources);
		result.addAll(changedOrAddedResources);
		return result.toArray(new IResource[result.size()]);
	}

	public List<IResource> computeChangeSet() {
		ArrayList<IResource> changeSet = new ArrayList<IResource>();
		changeSet.addAll(deletedResources);
		changeSet.addAll(changedOrAddedResources);
		return changeSet;
	}

	public void setProject(IProject project) {
		this.project = project;
	}

	public IProject getProject() {
	    return project;
	}

	public void setConsole(IProcessConsole console) {
		this.console = console;
	}

	/**
	 * Sets the resources delta, which is used to produce change sets.
	 *
	 * @param delta
	 *            May be null, which indicates a full build and in which case a
	 *            full visit is performed
	 * @throws CoreException
	 */
	public void setDiff(IFileTreeDiff diff) throws CoreException {
		changedOrAddedResources.clear();
		deletedResources.clear();
		if (diff == null) {
			project.accept(this);
		} else {
		    for (IPath added : diff.getAdded()) {
                addChangedOrAddedResource(project.findMember(added));
            }
		    for (IPath changed : diff.getChanged()) {
                addChangedOrAddedResource(project.findMember(changed));
            }
		    for (IPath removed : diff.getRemoved()) {
                addDeletedResource(project.getFile(removed));
            }
		}
	}

	/**
	 * Checks whether a resource affects this visitor's build process; if not,
	 * then it will not show up as a changed, added or deleted resource.
	 *
	 * @param resource
	 * @return
	 */
	public boolean doesAffectBuild(IResource resource) {
	    if (resource == null || resource.isDerived() || MoSyncBuilder.isInOutput(resource.getProject(), resource)) {
	        return false;
	    }
		// TODO: Make this much faster!
		MoSyncProject project = MoSyncProject.create(this.project);
		IFilter<IResource> resourceFilter = this.resourceFilter;
		if (resourceFilter == null) {
			resourceFilter = MoSyncProject.getExclusionFilter(project, true);
		}
		return project == null || resourceFilter.accept(resource);
	}

	/**
	 * <p>Checks whether a resource is an actual part of the build; or actually
	 * passed into a tool/compiler of some kind.
	 * The difference between this and <code>doesAffectBuild</code>
	 * is that only resources that get a <code>true</code> from
	 * this method will be included in the results returned from
	 * <code>computeResourcesToRebuild()</code>.</p>
	 * <p>The default implementation just passes it to
	 * doesAffectBuild.</p>
	 * @param resource
	 * @return
	 */
	public boolean isBuildable(IResource resource) {
		return doesAffectBuild(resource);
	}

	public Set<IResource> computeResourcesToRebuild(
			DependencyManager<IResource> dependencies) {
		Set<IResource> recompileThese = new HashSet<IResource>(
				changedOrAddedResources);
		List<IResource> changeSet = computeChangeSet();

		if (dependencies != null) {
			Set<IResource> reverseDependencies = dependencies
					.getReverseDependenciesOf(changeSet,
							DependencyManager.DEPTH_INFINITE);
			if (CoreMoSyncPlugin.getDefault().isDebugging()) {
				CoreMoSyncPlugin.trace("Change set: " + changeSet);
				CoreMoSyncPlugin.trace("Trigger recompile of: "
						+ reverseDependencies);
			}
			recompileThese.addAll(reverseDependencies);
			recompileThese.removeAll(deletedResources);
		}

		Set<IResource> result = new HashSet<IResource>();
		for (IResource recompileThis : recompileThese) {
			if (isBuildable(recompileThis)) {
				result.add(recompileThis);
			}
		}

		return result;
	}

	public void setVariant(IBuildVariant variant) {
		this.variant = variant;
	}

	protected IBuildVariant getVariant() {
	    return variant;
	}

	public IPropertyOwner getBuildProperties() {
		return MoSyncBuilder.getPropertyOwner(MoSyncProject.create(project), variant.getConfigurationId());
	}

	public void setResourceFilter(IFilter<IResource> resourceFilter) {
		this.resourceFilter = resourceFilter;
	}

	public void setDependencyProvider(IDependencyProvider<IResource> dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
	}

	public IDependencyProvider<IResource> getDependencyProvider() {
		return dependencyProvider;
	}

	public void setParameterResolver(ParameterResolver resolver) {
		this.resolver = resolver;
	}

	protected ParameterResolver getParameterResolver() {
		return resolver;
	}

	protected void resolvePaths(IPath[] paths) throws ParameterResolverException {
		MoSyncBuilder.resolvePaths(paths, resolver);
	}

	protected String resolve(String value) throws ParameterResolverException {
		return Util.replace(value, resolver);
	}
}
