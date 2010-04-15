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

import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import com.mobilesorcery.sdk.internal.dependencies.DependencyManager;

/**
 * <p>An auxiliary class for handling build state across hundreds
 * of different devices.</p>
 * <p>Since Eclipse natively only supports one delta state per
 * builder, we need to augment this to one build state per
 * <code>IBuildVariant</code></p>
 * <p>Clients may call this class to store and load build state
 * to extract build deltas on a per-buildvariant basis.</p>
 * <p>The information persisted by this class includes the
 * entire file tree used during building with it's timestamp;
 * this will limit the need for file traversal as well as gracefully
 * handling deleted files.</p>
 * <p>Also, the contents of the file is not meant to be used for long-term storage,
 * it's just there to temporarily aid the dependency analysis.</p>
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 *
 */
public interface IBuildState {

    public abstract void load();

    /**
     * Returns <code>true</code> if this build state was properly loaded.
     * (Or if setValid was called with a <code>true</code> value)
     * @return
     */
    public abstract boolean isValid();
    
    /**
     * Sets the valid state returned by isValid.
     * @param valid
     */
    public abstract void setValid(boolean valid);

    public abstract void save();

    /**
     * <p>Updates the state of [a] particular resource[s];
     * this information will be used be subsequent builds.</p>
     * <p>If the resource is a container, then the state of
     * the container's children will be updated recursively.</p>
     * @param resource
     * @throws CoreException 
     */
    public abstract void updateState(IResource resource) throws CoreException;

    /**
     * <p>Updates the state of [a] particular resource[s] contained in a diff;
     * this information will be used be subsequent builds.</p>
     */
    public abstract void updateState(IFileTreeDiff diff);

    /**
     * Updates the build result, typically <emph>after</emph>
     * having performed a build.
     * @param result
     */
    public abstract void updateResult(IBuildResult buildResult);
    
    /**
     * <p>Updates the properties used for this build (will serve as a nice log
     * of 'what did I build with'.</p>
     * @param properties
     */
    public abstract void updateBuildProperties(Map<String, String> properties);

    /**
     * Returns the variant this build state is applicable for.
     * @return
     */
    public abstract IBuildVariant getBuildVariant();

    public abstract boolean fullRebuildNeeded();

    /**
     * Returns a reference the dependency manager of this build state.
     * Clients may modify the state of the dependency manager.
     * @return
     */
    public abstract DependencyManager<IResource> getDependencyManager();

    /**
     * Creates a diff between this build state
     * and the resource tree of the project associated
     * with this build state
     * @return
     * @throws CoreException If the project could not be traversed (eg, the project is closed)
     */
    public abstract IFileTreeDiff createDiff() throws CoreException;

    /**
     * A diff for the build properties used for this build state vs
     * the ones currently set for the project.
     */
    public abstract Set<String> getChangedBuildProperties();
    
    /**
     * Clears all build state. Calling <code>fullRebuildNeeded</code>
     * right after this method will return <code>true</code>.
     */
    public abstract void clear();


}