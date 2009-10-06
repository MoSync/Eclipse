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
package com.mobilesorcery.sdk.internal.dependencies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.IInclude;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.internal.core.model.CModelManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;

public class CResourceDependencyProvider implements IDependencyProvider<IResource> {

	public static List<IResource> computeDependenciesOf(List<IResource> resources) throws CModelException {
		ArrayList<IResource> result = new ArrayList<IResource>();
		for (IResource resource : resources) {
			computeDependenciesOf(resource, result);
		}
		
		return result;
	}

	public static void computeDependenciesOf(IResource resource, List<IResource> result) throws CModelException {
        ICElement cfile = CModelManager.getDefault().create(resource, null);
        if (cfile == null) {
        	return;
        }
        
        ITranslationUnit tu = (ITranslationUnit) cfile.getAdapter(ITranslationUnit.class);
        
        if (tu == null) {
        	return;
        }
        
        IInclude[] includes = tu.getIncludes();
        
        for (int i = 0; i < includes.length; i++) {
            IWorkspace ws = ResourcesPlugin.getWorkspace();
            IFile[] files = ws.getRoot().findFilesForLocation(new Path(includes[i].getFullFileName()));

            result.addAll(Arrays.asList(files));
        }
        
        if (CoreMoSyncPlugin.getDefault().isDebugging()) {
        	CoreMoSyncPlugin.trace("Resolved dependencies of " + resource + ": " + result);
        }
	}

	public Map<IResource, Collection<IResource>> computeDependenciesOf(IResource resource) throws CModelException {
		ArrayList<IResource> dependencies = new ArrayList<IResource>();
		computeDependenciesOf(resource, dependencies);
		Map<IResource, Collection<IResource>> result = new HashMap<IResource, Collection<IResource>>();
		result.put(resource, dependencies);
		return result;
	}
	
	
}
