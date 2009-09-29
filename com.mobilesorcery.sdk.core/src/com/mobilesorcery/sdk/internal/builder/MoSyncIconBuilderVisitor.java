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
			return resource.getName().endsWith(".icon");
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