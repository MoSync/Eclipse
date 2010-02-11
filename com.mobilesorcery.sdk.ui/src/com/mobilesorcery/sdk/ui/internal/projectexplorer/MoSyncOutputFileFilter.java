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
package com.mobilesorcery.sdk.ui.internal.projectexplorer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
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
