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
package com.mobilesorcery.sdk.internal;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IContainerEntry;
import org.eclipse.cdt.core.model.IPathEntry;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.internal.cdt.MoSyncIncludePathContainer;

public class ReindexListener implements PropertyChangeListener {

	public class SetPathEntriesRunnable implements IWorkspaceRunnable {

		private ICProject cProject;
		private IPathEntry[] includePaths;

		public void setRawPathEntries(ICProject cProject, IPathEntry[] includePaths ) {
			this.cProject = cProject;
			this.includePaths = includePaths;
		}
		
		public void run(IProgressMonitor monitor) throws CoreException {
			CoreModel.setRawPathEntries(cProject, includePaths, monitor);
			CCorePlugin.getIndexManager().reindex(cProject);
			
			// TODO: A bit crude, but unless we do this the include paths & compiler symbols will
			// not be updated in UI. (Should be enough with workspace op)
			cProject.getProject().close(monitor);
			cProject.getProject().open(monitor);
		}

	}

	public void propertyChange(PropertyChangeEvent event) {
		try {
			if (propertyIsApplicable(event.getPropertyName())) {
				Object source = event.getSource();
				if (source instanceof MoSyncProject) {
					MoSyncProject project = (MoSyncProject) source;
					IProject wrappedProject = project.getWrappedProject();
					ICProject cProject = CoreModel.getDefault().create(
							wrappedProject);

					IContainerEntry includePaths = CoreModel
							.newContainerEntry(MoSyncIncludePathContainer.CONTAINER_ID);
					
					IWorkspace ws = wrappedProject.getWorkspace();
					SetPathEntriesRunnable setPathEntriesRunnable = new SetPathEntriesRunnable();
					setPathEntriesRunnable.setRawPathEntries(cProject, new IPathEntry[] { includePaths });
					
					ws.run(setPathEntriesRunnable, ws.getRoot(), IWorkspace.AVOID_UPDATE, null);					
					
					// CCorePlugin.getIndexManager().joinIndexer(arg0, arg1);
				}
			} 
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private boolean propertyIsApplicable(String propertyName) {
		return MoSyncBuilder.ADDITIONAL_INCLUDE_PATHS == propertyName
		|| MoSyncBuilder.IGNORE_DEFAULT_INCLUDE_PATHS == propertyName
		|| MoSyncBuilder.EXTRA_COMPILER_SWITCHES == propertyName
		|| MoSyncProject.BUILD_CONFIGURATION_CHANGED == propertyName
		|| MoSyncProject.BUILD_CONFIGURATION_SUPPORT_CHANGED == propertyName;
	}
}
