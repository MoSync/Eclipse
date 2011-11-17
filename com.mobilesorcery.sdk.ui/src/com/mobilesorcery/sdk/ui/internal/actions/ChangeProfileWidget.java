package com.mobilesorcery.sdk.ui.internal.actions;

import java.beans.PropertyChangeEvent;
import java.text.MessageFormat;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.filter.CompositeDeviceFilter;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.ProfileLabelProvider;
import com.mobilesorcery.sdk.ui.UIUtils;

public class ChangeProfileWidget extends MoSyncProjectWidget {

	private boolean active = true;

    @Override
	protected Control createControl(Composite parent) {
    	super.attachListeners();
        ComboViewer combo = new ComboViewer(parent, SWT.BORDER | SWT.READ_ONLY);
        combo.addSelectionChangedListener(this);
        combo.getControl().setLayoutData(new GridData(UIUtils.getDefaultFieldSize(), SWT.DEFAULT));
        this.ui = combo;
        updateUI(true);
        return combo.getControl();
    }

    @Override
	public boolean shouldUpdateProject(PropertyChangeEvent event) {
    	String prop = event.getPropertyName();
        return  MosyncUIPlugin.CURRENT_PROJECT_CHANGED == prop ||
        		IDeviceFilter.FILTER_CHANGED == prop ||
        		CompositeDeviceFilter.FILTER_ADDED == prop ||
        		CompositeDeviceFilter.FILTER_REMOVED == prop;
    }

    @Override
	public void selectionChanged(SelectionChangedEvent event) {
    	if (active) {
	    	active = false;
	        IStructuredSelection selection = (IStructuredSelection) event.getSelection();
	        Object selected = selection.getFirstElement();
	        if (selected instanceof IProfile) {
	        	project.setTargetProfile((IProfile) selected);
	        }
	        updateUI();
	        Display.getCurrent().asyncExec(new Runnable() {
				@Override
				public void run() {
			        active = true;
				}
			});
    	}
    }

    @Override
	public void updateUI() {
    	updateUI(false);
    }

    public void updateUI(boolean force) {
    	boolean activeCombo = project != null;
        ComboViewer combo = (ComboViewer) ui;
        if (activeCombo || force) {
        	combo.setContentProvider(new ProfileListContentProvider());
            combo.setLabelProvider(new ProfileLabelProvider(SWT.FLAT));
            combo.setInput(project == null ? MoSyncTool.getDefault() : project);
            IProfile profile = project == null ? null : project.getTargetProfile();
            combo.setSelection(profile == null ? new StructuredSelection() : new StructuredSelection(profile));
            String name = project == null ? "" : project.getName();
            if (profile != null) {
            	combo.getCombo().setText(profile.getName() + " (" + profile.getVendor().getName() + ")");
            }
            combo.getControl().setToolTipText(MessageFormat.format("Set target profile for project {0}", name));
        } else {
            noProjectSelected();
        }
        combo.getControl().setEnabled(activeCombo);
	}

}
