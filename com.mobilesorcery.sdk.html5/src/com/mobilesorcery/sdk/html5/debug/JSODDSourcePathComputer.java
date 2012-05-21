package com.mobilesorcery.sdk.html5.debug;

import java.util.ArrayList;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;
import org.eclipse.debug.core.sourcelookup.containers.FolderSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ProjectSourceContainer;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.html5.Html5Plugin;

public class JSODDSourcePathComputer implements ISourcePathComputerDelegate {

	@Override
	public ISourceContainer[] computeSourceContainers(
			ILaunchConfiguration configuration, IProgressMonitor monitor)
			throws CoreException {
		// TODO: Project in launch config!?
		IProject[] allProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		ArrayList<ISourceContainer> sourceContainers = new ArrayList<ISourceContainer>();
		for (IProject project : allProjects) {
			MoSyncProject mosyncProject = MoSyncProject.create(project);
			if (mosyncProject != null) {
				if (Html5Plugin.getDefault().hasHTML5Support(mosyncProject)) {
					// TODO: Should this really be hardcoded here?
					IFolder jsFolder = project.getFolder("LocalFiles");
					if (jsFolder.exists()) {
						sourceContainers.add(new FolderSourceContainer(jsFolder, true));
					}
				}
			}
		}
		return sourceContainers.toArray(new ISourceContainer[sourceContainers.size()]);
	}

}
