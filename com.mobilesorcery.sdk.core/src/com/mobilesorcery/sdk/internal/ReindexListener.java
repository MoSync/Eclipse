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
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IContainerEntry;
import org.eclipse.cdt.core.model.IPathEntry;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.internal.cdt.MoSyncIncludePathContainer;

/**
 * Listens for changes in the include paths in a MoSync project,
 * and updates them in the corresponding CDT project.
 * 
 * @author fmattias
 */
public class ReindexListener implements PropertyChangeListener
{	
	/**
	 * Updates and reindexes the include paths for an underlying CDT-project.
	 * 
	 * @author fmattias
	 */
	public class UpdateAndIndexIncludePathsJob extends WorkspaceJob
	{
		/**
		 * The project to index the include paths for.
		 */
		private ICProject m_cProject;
		
		/**
		 * The new include paths to index.
		 */
		private IPathEntry[] m_includePaths;
		
		/**
		 * Constructor.
		 * 
		 * @param cProject The project to update with new include paths.
		 * @param includePaths The new include paths to set 
		 */
		public UpdateAndIndexIncludePathsJob(ICProject cProject, IPathEntry[] includePaths)
		{
			super("Updating includes for: " + cProject.getElementName());
			
			m_cProject = cProject;
			m_includePaths = includePaths;
		}

		/**
		 * Update and reindex include paths.
		 */
		@Override
		public IStatus runInWorkspace(IProgressMonitor monitor)
				throws CoreException
		{
			CoreModel.setRawPathEntries(m_cProject, m_includePaths, null);

			// The indexing can be long running, but it will be performed in
			// the background by the index manager
			CCorePlugin.getIndexManager().reindex(m_cProject);

			return Status.OK_STATUS;
		}
	}

	public void propertyChange(PropertyChangeEvent event)
	{
		// Ensure the event is sent from a MoSync project
		if(!eventSourceIsFromMoSync(event.getSource()))
		{
			return;
		}
		
		// Check that it is a property that affects the include paths
		if (!needToUpdateIncludes(event.getPropertyName()))
		{
			return;
		}
		
		MoSyncProject project = (MoSyncProject) event.getSource();
		IProject wrappedProject = project.getWrappedProject();
		ICProject cProject = CoreModel.getDefault().create(wrappedProject);

		IContainerEntry includePaths = CoreModel
				.newContainerEntry(MoSyncIncludePathContainer.CONTAINER_ID);
		UpdateAndIndexIncludePathsJob job = 
			new UpdateAndIndexIncludePathsJob(cProject,  new IPathEntry[] { includePaths });

		// CDT locks the whole workspace when updating the includes, and since
		// it is running in our job the scope must be at least as big.
		job.setRule(ResourcesPlugin.getWorkspace().getRoot());
		job.schedule();
	}

	/**
	 * Determines if the given property change, affects the include paths.
	 * 
	 * @param propertyName The name of the property.
	 * 
	 * @return true if the given property affects the include paths,
	 *         false otherwise.
	 */
	private boolean needToUpdateIncludes(String propertyName)
	{
		// Using startsWith since each property can end with the configuration type
		return propertyName.startsWith(MoSyncBuilder.ADDITIONAL_INCLUDE_PATHS)
			|| propertyName.startsWith(MoSyncBuilder.IGNORE_DEFAULT_INCLUDE_PATHS)
			|| propertyName.startsWith(MoSyncBuilder.EXTRA_COMPILER_SWITCHES)
			|| propertyName.startsWith(MoSyncProject.BUILD_CONFIGURATION_CHANGED)
			|| propertyName.startsWith(MoSyncProject.BUILD_CONFIGURATION_SUPPORT_CHANGED);
	}
	
	/**
	 * Determines if the event source is a mosync project.
	 * 
	 * @param eventSource The object that sent the event.
	 * 
	 * @return true if eventSource was a MoSync project,
	 *         false otherwise.
	 */
	private boolean eventSourceIsFromMoSync(Object eventSource)
	{
		return eventSource instanceof MoSyncProject;
	}
}
