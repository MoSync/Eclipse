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
package com.mobilesorcery.sdk.wizards;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.templates.ITemplate;
import com.mobilesorcery.sdk.core.templates.ProjectTemplate;
import com.mobilesorcery.sdk.core.templates.ProjectTemplateDescription;
import com.mobilesorcery.sdk.core.templates.Template;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.mobilesorcery.sdk.wizards";

    public static final String RESOURCE_TEMPLATE_ID = "template.resource";

	// The shared instance
	private static Activator plugin;

    private TreeMap<String, ITemplate> templates = new TreeMap<String, ITemplate>();

    private TreeMap<String, ProjectTemplate> projectTemplates = new TreeMap<String, ProjectTemplate>();
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		initTemplates();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
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
						getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
					}
				}
			}
		}
		
        addTemplate(new Template(RESOURCE_TEMPLATE_ID, "Resource File",  "New Resource File", getClass().getResource("/templates/resources.lst.template"), "resource.lst", null));
	}
	
    private void addProjectTemplate(ProjectTemplate template) {
        projectTemplates.put(template.getId(), template);
    }

    private void addTemplate(ITemplate template) {
    	// We'll keep these for a while - but we should really really try to incorporate it
    	// in the CDT code templates list.
        templates.put(template.getId(), template);
    }

	public ProjectTemplate[] getProjectTemplates() {
	    return projectTemplates.values().toArray(new ProjectTemplate[0]);
	}
	
	public ITemplate getTemplate(String id) {
	    return templates.get(id);
	}
	
	
}
