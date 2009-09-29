package com.mobilesorcery.sdk.ui.internal.projectexplorer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import com.mobilesorcery.sdk.core.MoSyncNature;

public class MoSyncOutputFileFilter extends ViewerFilter {

	public MoSyncOutputFileFilter() {
	}

	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof IAdaptable) {
			element = ((IAdaptable) element).getAdapter(IResource.class);
		}
		
		if (element instanceof IFile || element instanceof IFolder) {
			IResource file = (IResource) element;
			IProject project = file.getProject();
			try {
				if (project.hasNature(MoSyncNature.ID)) {
					IPath projPath = file.getProjectRelativePath();
					if (projPath.equals(project.getProjectRelativePath().append("Output"))) {
						return false;
					}

					if (projPath.equals(project.getProjectRelativePath().append("FinalOutput"))) {
						return false;
					}

					String ext = file.getFileExtension();

					if ("s".equals(ext) || "tab".equals(ext) || "mopro".equals(ext) || "vcproj".equals(ext)) {
						return false;
					}
					
					if ("stabs.lst".equals(file.getName())) {
						return false;
					}
				}
			} catch (CoreException e) {
				// Ignore.
			}
		}

		return true;
	}

}
