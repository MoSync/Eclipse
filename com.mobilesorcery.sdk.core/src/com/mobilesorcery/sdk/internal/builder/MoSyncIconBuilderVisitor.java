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

import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.internal.dependencies.DependencyManager;

public class MoSyncIconBuilderVisitor extends IncrementalBuilderVisitor {

	class IconFileVisitor implements IResourceVisitor {
		ArrayList<IResource> iconFiles = new ArrayList<IResource>();
		
		public boolean visit(IResource resource) throws CoreException {
			if (doesAffectBuild(resource)) {
				iconFiles.add(resource);
			}
			
			return true;
		}
		
	}

	public boolean doesAffectBuild(IResource resource) {
		if (resource.getType() == IResource.FILE) {
			return resource.getName().endsWith(".icon") && super.doesAffectBuild(resource);
		}
    	return false;
    }

    public void incrementalCompile(IProgressMonitor monitor, DependencyManager<IResource> dependencyManager) throws CoreException {
    	IResource[] iconFiles = getChangedOrAddedResources();
    	if (iconFiles.length > 0) {
    		IResource iconFile = iconFiles[0];
        	MoSyncProject mosyncProject = MoSyncProject.create(project);
            DefaultPackager internal = new DefaultPackager(mosyncProject, mosyncProject.getTargetProfile(), false);
        	try {
            	internal.runCommandLine("%mosync-bin%\\icon-injector.exe", "-src", iconFile.getLocation().toOSString(), "-size", "16x16", "-platform", "more", "-dst", "%compile-output-dir%\\more.png");                                            		
        	} catch(IOException e) {
       	       throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, e.getMessage()));
        	}    	
    	}
    }
    
    protected String getName() {
		return "Icon Compiler";
	}

	public IResource[] getIconFiles() throws CoreException {
		IconFileVisitor visitor = new IconFileVisitor();
		project.accept(visitor);
		return visitor.iconFiles.toArray(new IResource[0]);
	}


}