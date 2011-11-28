package com.mobilesorcery.sdk.ui.internal.actions;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;

public abstract class MoSyncProjectWidget extends WorkbenchWindowControlContribution implements
PropertyChangeListener, IUpdatableControl {

    protected MoSyncProject project;

    protected MoSyncProject getProject() {
    	return project;
    }

    protected void noProjectSelected() {
    }

    @Override
	public void propertyChange(PropertyChangeEvent event) {
        // Project changed
        if (shouldUpdateProject(event)) {
        	project = MosyncUIPlugin.getDefault().getCurrentlySelectedProject(getWorkbenchWindow());
        	updateUI();
        }
    }

    protected abstract boolean shouldUpdateProject(PropertyChangeEvent event);

    @Override
	public void dispose() {
    	detachListeners();
        super.dispose();
    }

	protected void attachListeners() {
        MosyncUIPlugin.getDefault().addListener(this);
        MoSyncProject.addGlobalPropertyChangeListener(this);
	}

	protected void detachListeners() {
        MosyncUIPlugin.getDefault().removeListener(this);
        MoSyncProject.removeGlobalPropertyChangeListener(this);
	}
}
