package com.mobilesorcery.sdk.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.mobilesorcery.sdk.core.MoSyncProject;

public class BuildConfigurationsContentProvider implements IStructuredContentProvider, PropertyChangeListener {

	private MoSyncProject project;

	public BuildConfigurationsContentProvider(MoSyncProject project) {
		this.project = project;
		project.addPropertyChangeListener(this);
	}

	public Object[] getElements(Object inputElement) {
		return project.isBuildConfigurationsSupported() ? project.getBuildConfigurations().toArray() : new Object[0];
	}

	public void dispose() {
		project.removePropertyChangeListener(this);
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		
	}

	public void propertyChange(PropertyChangeEvent event) {
	}


}
