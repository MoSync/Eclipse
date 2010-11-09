package com.mobilesorcery.sdk.profiling.emulator;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;

import com.mobilesorcery.sdk.profiling.ILocationProvider;

public class DefaultLocationProvider implements ILocationProvider {

	private IWorkspaceRoot wsRoot;
	private IProject preferredProject;

	/**
	 * Creates a new <code>DefaultLocationProvider</code>
	 * @param preferredProject If a path matches several <code>IFile</code>s,
	 * then the <code>IFile</code>s contained by <code>preferredProject</code>
	 * will be used (if any).
	 */
	public DefaultLocationProvider(IProject preferredProject) {
		wsRoot = ResourcesPlugin.getWorkspace().getRoot();
	}
	
	public IFile getLocation(Object element) {
		if (element instanceof String) {
			element = new File((String) element);
		}
		
		if (element instanceof File) {
			Path path = new Path(((File) element).getAbsolutePath());
			IFile[] files = wsRoot.findFilesForLocation(path);
			if (files.length > 0) {
				if (files.length == 1 || preferredProject == null) {
					return files[0];
				} else if (files.length > 1) {
					int preferredIx = 0;
					for (int i = 0; i < files.length; i++) {
						if (preferredProject.equals(files[i].getProject())) {
							preferredIx = i;
						}
					}
					
					return files[preferredIx];
				}
			}
		} else if (element instanceof IFile) {
			return (IFile) element;
		}
		
		return null;
	}

}
