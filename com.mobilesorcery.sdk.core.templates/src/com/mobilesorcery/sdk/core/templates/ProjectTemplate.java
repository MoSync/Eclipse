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
package com.mobilesorcery.sdk.core.templates;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;

public class ProjectTemplate {

	private ProjectTemplateDescription desc;
	private File templateRoot;

	public ProjectTemplate(File templateRoot, ProjectTemplateDescription desc) {
		// TODO: Should support not only file base templates!?
		this.templateRoot = templateRoot;
		this.desc = desc;
	}

	public String getId() {
		return desc.getId();
	}
	
	public String getName() {
		return desc.getName();
	}

	public String getDescription() {
		return desc.getDescriptionText();
	}
	
	public String getType() {
		return desc.getType();
	}

	public String validate() {
		if (getId() == null) {
			return missingAttr("id"); //$NON-NLS-1$
		}
		
		if (getName() == null) {
			return missingAttr("name"); //$NON-NLS-1$
		}
		
		if (!templateRoot.isDirectory() || !templateRoot.exists()) {
			return Messages.ProjectTemplate_InvalidTemplateRoot;
		}
		
		return null;
	}
	
	private String missingAttr(String attr) {
		return MessageFormat.format(Messages.ProjectTemplate_InvalidTemplate_0, attr);
	}

	/**
	 * Initializes a project (it has to exist and be open); creates a set of files
	 * and sets any specific settings 
	 * @param monitor
	 * @param mosyncProject
	 * @return
	 * @throws CoreException
	 */
	public IFile initalizeProject(IProgressMonitor monitor, MoSyncProject mosyncProject) throws CoreException {
		IProject project = mosyncProject.getWrappedProject();
		
		if (!project.exists()) {
			throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, Messages.ProjectTemplate_ProjectInitFailed_0));
		}

		if (!project.isOpen()) {
			throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, Messages.ProjectTemplate_ProjectInitFailed_1));
		}
				
		List<String> templateFiles = desc.getTemplateFiles();
		List<String> generatedFiles = desc.getGeneratedFiles();
		for (int i = 0; i < templateFiles.size(); i++) {
			String templateFile = templateFiles.get(i);
			String generatedFile = generatedFiles.get(i);
			IFile file = project.getFile(new Path(generatedFile));
			FileInputStream fis = null;
	        try {
	        	File templateFileLocation = new File(templateRoot, templateFile);
	        	fis = new FileInputStream(templateFileLocation);
	        	// TODO: Parameters!
	            file.create(fis, true, new SubProgressMonitor(monitor, 1));                
	        } catch (IOException e) {
	            throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, Messages.ProjectTemplate_ProjectInitFailed_2, e));
	        } finally {
        		Util.safeClose(fis);
	        }
		}
        
		Map<String, String> settings = desc.getSettings();
        if (settings != null) {
            mosyncProject.setProperties(settings);
        }
        
        mosyncProject.setProperty(MoSyncProject.TEMPLATE_ID, getId());

        if (generatedFiles.size() > 0) {
        	return project.getFile(new Path(generatedFiles.get(0)));
        }
        
        return null;
	}


}
