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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.launch.IEmulatorLauncher;
import com.mobilesorcery.sdk.core.templates.ITemplate;
import com.mobilesorcery.sdk.core.templates.ProjectTemplate;
import com.mobilesorcery.sdk.core.templates.ProjectTemplateDescription;
import com.mobilesorcery.sdk.core.templates.Template;

/**
 * The activator class controls the plug-in life cycle
 */
public class TemplateManager {

    public static final String RESOURCE_TEMPLATE_ID = "template.resource";

	// The shared instance
	private static TemplateManager INSTANCE;

    private final TreeMap<String, ITemplate> templates = new TreeMap<String, ITemplate>();

    private final TreeMap<String, ProjectTemplate> projectTemplates = new TreeMap<String, ProjectTemplate>();

	private TreeMap<String, IProjectTemplateExtension> extensions = null;

	public final static synchronized TemplateManager getDefault() {
		if (INSTANCE == null) {
			INSTANCE = new TemplateManager();
		}
		return INSTANCE;
	}

	/**
	 * The constructor
	 */
	private TemplateManager() {
		initTemplates();
	}

	private void initTemplates() {
		IPath templateDir = MoSyncTool.getDefault().getTemplatesPath();
		File[] subdirs = templateDir.toFile().listFiles();
		for (int i = 0; i < subdirs.length; i++) {
			File subdir = subdirs[i];
			if (subdir.isDirectory()) {
				File descFile = new File(subdir, "project.desc");
				if (descFile.exists()) {
					try {
						ProjectTemplateDescription desc = ProjectTemplateDescription.parse(descFile);
						ProjectTemplate template = new ProjectTemplate(subdir, desc);
						String validationResult = template.validate();
						if (validationResult == null) {
							addProjectTemplate(template);
						} else {
							// Just to end up in the catch clause.
							throw new IOException("Invalid template: " + validationResult);
						}
					} catch (IOException e) {
						CoreMoSyncPlugin.getDefault().log(e);
					}
				}
			}
		}

        addTemplate(new Template(RESOURCE_TEMPLATE_ID, "Resource File",  "New Resource File", getClass().getResource("/templates/resources.lstx.template"), "Resources/resource.lstx", null));
	}

    private void addProjectTemplate(ProjectTemplate template) {
        projectTemplates.put(template.getId(), template);
    }

    private void addTemplate(ITemplate template) {
    	// We'll keep these for a while - but we should really really try to incorporate it
    	// in the CDT code templates list.
        templates.put(template.getId(), template);
    }

	public List<ProjectTemplate> getProjectTemplates(String type) {
		ArrayList<ProjectTemplate> result = new ArrayList<ProjectTemplate>();
		for (ProjectTemplate projectTemplate : projectTemplates.values()) {
			if (Util.equals(type, projectTemplate.getType())) {
				result.add(projectTemplate);
			}
		}
	    return result;
	}

	public IProjectTemplateExtension getExtensionForType(String type) {
		if (extensions == null) {
			extensions = new TreeMap<String, IProjectTemplateExtension>();
	    	IConfigurationElement[] extensionCfgs = Platform.getExtensionRegistry().getConfigurationElementsFor(ProjectTemplateExtension.EXTENSION_POINT_ID);
	    	for (IConfigurationElement extensionCfg : extensionCfgs) {
	    		ProjectTemplateExtension extensionImpl = new ProjectTemplateExtension(extensionCfg);
	    		String extensionType = type = extensionImpl.getType();
	    		extensions.put(extensionType, extensionImpl);
	    	}
		}
		return extensions.get(type);
	}

	public Collection<String> getTemplateTypes() {
		HashSet<String> result = new HashSet<String>();
		for (ProjectTemplate projectTemplate : projectTemplates.values()) {
			result.add(projectTemplate.getType());
		}
		return result;
	}

	public ITemplate getTemplate(String id) {
	    return templates.get(id);
	}

	public ProjectTemplate getProjectTemplate(String id) {
		return projectTemplates.get(id);
	}


}
