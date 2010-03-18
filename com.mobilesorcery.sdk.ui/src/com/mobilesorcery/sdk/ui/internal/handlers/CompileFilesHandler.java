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
package com.mobilesorcery.sdk.ui.internal.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.handlers.HandlerUtil;

import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.ResourceFilter;
import com.mobilesorcery.sdk.ui.MoSyncCommandHandler;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;

public class CompileFilesHandler extends MoSyncCommandHandler {

	class CompileFilesJob extends Job {

		private List<IResource> resources;

		public CompileFilesJob() {
			super("Quick Compile");
		}

		public void setResources(List<IResource> resources) {
			this.resources = resources;
		}

		protected IStatus run(IProgressMonitor monitor) {
			try {
				HashMap<MoSyncProject, List<IResource>> projectResourceMap = new HashMap<MoSyncProject, List<IResource>>();

				int resourceCount = 0;

				MoSyncBuilder builder = new MoSyncBuilder();
				for (IResource resource : resources) {
					MoSyncProject project = MoSyncProject.create(resource
							.getProject());
					if (project != null) {
						List<IResource> resourceList = projectResourceMap
								.get(project);
						if (resourceList == null) {
							resourceList = new ArrayList<IResource>();
							projectResourceMap.put(project, resourceList);
						}
						resourceList.add(resource);
						resourceCount++;
					}
				}

				monitor.beginTask("Quick Compile", resourceCount);

				for (MoSyncProject project : projectResourceMap.keySet()) {
					List<IResource> resources = projectResourceMap.get(project);
					HashSet<IResource> resourceSet = new HashSet<IResource>(
							resources);
					ResourceFilter filter = new ResourceFilter(resourceSet);

					builder.fullBuild(project.getWrappedProject(), project.getTargetProfile(), false,
							false, false, false, filter, false, new SubProgressMonitor(
									monitor, resourceSet.size()));
				}
				return Status.OK_STATUS;
			} catch (Exception e) {
				return new Status(IStatus.ERROR, MosyncUIPlugin.PLUGIN_ID, "Quick Compile failed.", e);
			}
		}

	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<IResource> resources = extractResources(HandlerUtil
				.getCurrentSelection(event));

		CompileFilesJob job = new CompileFilesJob();
		job.setResources(resources);
		job.schedule();
		return null;
	}

}
