/*  Copyright (C) 2010 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.mobilesorcery.sdk.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.MoSyncProject;

public class BuildConfigurationsContentProvider implements IStructuredContentProvider, PropertyChangeListener {

	private MoSyncProject project;
    private String[] buildConfigurationTypes;

    public BuildConfigurationsContentProvider(MoSyncProject project) {
        this(project, (String[])null);
    }
    
    public BuildConfigurationsContentProvider(MoSyncProject project, String... buildConfigurationTypes) {
		this.project = project;
		this.buildConfigurationTypes = buildConfigurationTypes;
		project.addPropertyChangeListener(this);
	}

	public Object[] getElements(Object inputElement) {
	    Set<String> cfgIds = buildConfigurationTypes == null ? project.getBuildConfigurations() : project.getBuildConfigurationsOfType(buildConfigurationTypes);
	    return project.areBuildConfigurationsSupported() ? cfgIds.toArray() : new Object[0];
	}

	public void dispose() {
		project.removePropertyChangeListener(this);
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		
	}

	public void propertyChange(PropertyChangeEvent event) {
	}


}
