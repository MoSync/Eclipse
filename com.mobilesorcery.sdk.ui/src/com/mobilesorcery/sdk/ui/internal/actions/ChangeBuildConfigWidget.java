package com.mobilesorcery.sdk.ui.internal.actions;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.ui.BuildConfigurationsContentProvider;
import com.mobilesorcery.sdk.ui.BuildConfigurationsLabelProvider;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;

public class ChangeBuildConfigWidget extends WorkbenchWindowControlContribution implements PropertyChangeListener, ISelectionChangedListener {

    private MoSyncProject project;
    private ComboViewer combo;

    protected Control createControl(Composite parent) {
        combo = new ComboViewer(parent, SWT.READ_ONLY);
        combo.addSelectionChangedListener(this);
        MosyncUIPlugin.getDefault().addListener(this);
        MoSyncProject.addGlobalPropertyChangeListener(this);
        updateProject();
        
        combo.getCombo().setItems( new String[] {"No project selected"} );
        combo.getCombo().select(0);
        
        return combo.getControl();
    }
    
    public void dispose() {
        MosyncUIPlugin.getDefault().removeListener(this);
        MoSyncProject.removeGlobalPropertyChangeListener(this);
        super.dispose();
    }

    public void propertyChange(PropertyChangeEvent event) {
        // Project changed
        if (MosyncUIPlugin.CURRENT_PROJECT_CHANGED == event.getPropertyName() || MoSyncProject.BUILD_CONFIGURATION_CHANGED == event.getPropertyName()) {
            updateProject();
        }
    }


    private void updateProject() {
        project = MosyncUIPlugin.getDefault().getCurrentlySelectedProject(getWorkbenchWindow());
        
        boolean activeCombo = project != null && project.areBuildConfigurationsSupported() && !project.getBuildConfigurations().isEmpty();
        if (activeCombo) {
            combo.setContentProvider(new BuildConfigurationsContentProvider(project));
            combo.setLabelProvider(new BuildConfigurationsLabelProvider(project, false));
            combo.setInput(project);
            IBuildConfiguration cfg = project.getActiveBuildConfiguration();
            combo.setSelection(cfg == null ? new StructuredSelection() : new StructuredSelection(cfg.getId()));
            combo.getControl().setToolTipText(MessageFormat.format("Set configuration for project {0}", project.getName()));
        } 
        combo.getControl().setEnabled(activeCombo);
    }

    public void selectionChanged(SelectionChangedEvent event) {
        IStructuredSelection selection = (IStructuredSelection) event.getSelection();
        String cfgId = (String) selection.getFirstElement();
        if (cfgId != null) {
            project.setActiveBuildConfiguration(cfgId);
        }
    }

}
