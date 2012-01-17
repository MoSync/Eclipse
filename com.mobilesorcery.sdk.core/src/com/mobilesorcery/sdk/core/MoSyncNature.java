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
package com.mobilesorcery.sdk.core;

import java.util.ArrayList;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IContainerEntry;
import org.eclipse.cdt.core.model.IPathEntry;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.internal.cdt.MoSyncIncludePathContainer;
import com.mobilesorcery.sdk.internal.cdt.MoSyncPathInitializer;

public class MoSyncNature implements IProjectNature {

	/**
	 * The MoSync project nature ID
	 */
    public static final String ID = CoreMoSyncPlugin.PLUGIN_ID + ".nature";

	/**
	 * The nature ID used in previous versions of MoSync IDE.
	 */
    public static final String COMPATIBLE_ID = "com.mobilesorcery.sdk.builder.nature";

    private IProject project;

    @Override
	public void configure() throws CoreException {
        addBuilder(project, MoSyncBuilder.ID);
        removeBuilder(project, MoSyncBuilder.COMPATIBLE_ID);
    }

    @Override
	public void deconfigure() throws CoreException {
    	removeBuilder(project, MoSyncBuilder.ID);
    	removeBuilder(project, MoSyncBuilder.COMPATIBLE_ID);
    }

    @Override
	public IProject getProject() {
        return project;
    }

    @Override
	public void setProject(IProject project) {
        this.project = project;
    }

    public static void addNatureToProject(IProject project, boolean modifyIncludePaths) {
        try {
            if (!project.isOpen()) {
                project.open(new NullProgressMonitor());
            }

            /*
             * if (project.hasNature(ID)) { return; }
             */

            addCNature(project);

            IProjectDescription description = project.getDescription();
            String[] natures = description.getNatureIds();
            ArrayList<String> newNatures = new ArrayList<String>();
            newNatures.add(ID);
            for (int i = 0; i < natures.length; i++) {
            	if (!natures[i].equals(COMPATIBLE_ID) && !natures[i].equals(ID)) {
            		newNatures.add(natures[i]);
            	}
            }

            description.setNatureIds(newNatures.toArray(new String[0]));
            project.setDescription(description, null);

            if (modifyIncludePaths) {
            	modifyIncludePaths(project);
            }
        } catch (CoreException e) {
            CoreMoSyncPlugin.getDefault().getLog().log(
                    new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, "Could not ass�gn project to be a MoSync Project", e));
        }
    }

    public static void modifyIncludePaths(IProject project) throws CoreException {
        ICProject cProject = CoreModel.getDefault().create(project);
        IContainerEntry includePaths = CoreModel.newContainerEntry(MoSyncIncludePathContainer.CONTAINER_ID);
        CoreModel.setRawPathEntries(cProject, new IPathEntry[] { includePaths }, new NullProgressMonitor());
        MoSyncPathInitializer.getInstance().initialize(MoSyncIncludePathContainer.CONTAINER_ID, cProject);
    }

    /*private static void modifyIncludePaths(IProject project) throws CoreException {
		ICProjectDescription projectDesc = CoreModel.getDefault().getProjectDescription(project);
		ICConfigurationDescription configDesc = projectDesc.getConfiguration();
		if (configDesc == null) {
		    projectDesc.createConfiguration(arg0, arg1)
		}
		LinkedHashSet<String> externalSettingsProviders = new LinkedHashSet<String>(Arrays.asList(configDesc.getExternalSettingsProviderIds()));
		externalSettingsProviders.add(MoSyncExternalSettingProvider.ID);
		configDesc.setExternalSettingsProviderIds(externalSettingsProviders.toArray(new String[0]));
        CoreModel.getDefault().setProjectDescription(project, projectDesc);
	}*/


    private static void addCNature(IProject project) throws CoreException {
        IProjectDescription description = project.getDescription();
        CCorePlugin.getDefault().createCProject(description, project, new NullProgressMonitor(), MoSyncProject.C_PROJECT_ID);
        CCProjectNature.addCCNature(project, new NullProgressMonitor());
    }

    private void addBuilder(IProject project, String builderID) throws CoreException {
        IProjectDescription desc = project.getDescription();
        ICommand[] commands = desc.getBuildSpec();
        for (int i = 0; i < commands.length; i++)
            if (commands[i].getBuilderName().equals(builderID))
                return;

        ICommand command = desc.newCommand();
        command.setBuilderName(builderID);
        ICommand[] nc = new ICommand[commands.length + 1];
        System.arraycopy(commands, 0, nc, 1, commands.length);
        nc[0] = command;
        desc.setBuildSpec(nc);
        project.setDescription(desc, null);
    }

    private void removeBuilder(IProject project, String builderID) throws CoreException {
        IProjectDescription desc = project.getDescription();
        ICommand[] commands = desc.getBuildSpec();
        ArrayList<ICommand> newCommands = new ArrayList<ICommand>();

        for (int i = 0; i < commands.length; i++) {
            if (!commands[i].getBuilderName().equals(builderID)) {
            	newCommands.add(commands[i]);
            }
        }

        desc.setBuildSpec(newCommands.toArray(new ICommand[0]));
        project.setDescription(desc, null);
    }

    /**
     * <p>Checks whether a project is a mosync project;
     * ie whether it nature that is compatible with
     * <code>MoSyncNature.ID</code>.</p>
     * @param project
     * @return
     * @throws CoreException If the operation fails (this could
     * happen if the project does not exist, if it is closed, etc)
     */
	public static boolean isCompatible(IProject project) throws CoreException {
		return project != null && (project.hasNature(MoSyncNature.ID) || project.hasNature(MoSyncNature.COMPATIBLE_ID));
	}

	public static boolean hasNature(IProject project) throws CoreException {
		return project.hasNature(MoSyncNature.ID);
	}

}
