package com.mobilesorcery.sdk.ui.internal.actions;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;

public abstract class MoSyncProjectWidget extends WorkbenchWindowControlContribution implements
PropertyChangeListener, ISelectionChangedListener, IUpdatableControl {

    protected MoSyncProject project;
    protected Object ui;

    @Override
	protected Control createControl(Composite parent) {
        ComboViewer ui = new ComboViewer(parent, SWT.READ_ONLY);
        ui.addSelectionChangedListener(this);
        attachListeners();
        this.ui = ui;
        updateUI();
        return ui.getControl();
    }

    protected MoSyncProject getProject() {
    	return project;
    }

    protected void noProjectSelected() {
        if (ui instanceof ComboViewer) {
        	ComboViewer combo = (ComboViewer) ui;
        	combo.getCombo().setItems( new String[] {"No project selected"} );
        	combo.getCombo().select(0);
        }
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
