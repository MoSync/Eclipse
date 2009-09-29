package com.mobilesorcery.sdk.ui.internal.launch;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.ILaunchConstants;
import com.mobilesorcery.sdk.core.MoSyncNature;

public class MoSyncLaunchParamsTab extends AbstractLaunchConfigurationTab {

    private class TabListener implements ModifyListener, SelectionListener {

        public void modifyText(ModifyEvent e) {
            updateLaunchConfigurationDialog();
        }

        public void widgetDefaultSelected(SelectionEvent e) {/* do nothing */
        }

        public void widgetSelected(SelectionEvent e) {
            Object source = e.getSource();
            if (source == projectButton) {
                handleProjectButtonSelected();
            } else {
                updateLaunchConfigurationDialog();
            }
        }
    }

    private Text projectText;
    private Button projectButton;
    private TabListener listener = new TabListener();
    private IProject project;
    private Text widthText;
    private Text heightText;
    private Button useTargetProfile;

    public void createControl(Composite parent) {
        Composite control = new Composite(parent, SWT.NONE);
        control.setLayout(new GridLayout());
        createProjectEditor(control);
        createResolutionEditor(control);
        setControl(control);
    }

    private IProject selectProject() {
        ILabelProvider labelProvider = new LabelProvider() {
            public String getText(Object o) {
                if (o instanceof IProject) {
                    return ((IProject) o).getName();
                }

                return "?";
            }
        };

        ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), labelProvider);
        dialog.setTitle("Select MoSync Project");
        dialog.setMessage("Select [an open] MoSync Project for this launch");

        try {
            dialog.setElements(getMoSyncProjects(getWorkspaceRoot()));
        } catch (CoreException e) {
            CoreMoSyncPlugin.getDefault().log(e);
        }
        
        IProject project = getProject();

        if (project != null) {
            dialog.setInitialSelections(new Object[] { project });
        }

        if (dialog.open() == Window.OK) {
            return (IProject) dialog.getFirstResult();
        }

        return null;
    }

    private void createResolutionEditor(Composite control) {
        Group screenSizeGroup = new Group(control, SWT.NONE);
        screenSizeGroup.setText("Screen Size");
        screenSizeGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        GridLayout layout = new GridLayout(2, false);
        screenSizeGroup.setLayout(layout);

        useTargetProfile = new Button(screenSizeGroup, SWT.CHECK);
        useTargetProfile.setText("If possible, &Use Screen Size of Currently Selected Target Profile");
        useTargetProfile.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));
        
        Label widthL = new Label(screenSizeGroup, SWT.NONE);
        widthL.setText("&Width");
        
        Label heightL = new Label(screenSizeGroup, SWT.NONE);
        heightL.setText("&Height");
        
        widthText = new Text(screenSizeGroup, SWT.BORDER | SWT.SINGLE); 
        heightText = new Text(screenSizeGroup, SWT.BORDER | SWT.SINGLE);
        widthText.setLayoutData(new GridData(110, SWT.DEFAULT));
        heightText.setLayoutData(new GridData(110, SWT.DEFAULT));
        
        widthText.addModifyListener(listener);
        heightText.addModifyListener(listener);
    }

    protected void handleProjectButtonSelected() {
        IProject project = selectProject();
        if (project == null) {
            return;
        }
        
        String projectName = project.getName();
        projectText.setText(projectName);     
    }
    
    private IWorkspaceRoot getWorkspaceRoot() {
        return ResourcesPlugin.getWorkspace().getRoot();
    }

    protected void createProjectEditor(Composite parent) {
        Font font = parent.getFont();
        Group group = new Group(parent, SWT.NONE);
        group.setText("Project");
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        group.setLayoutData(gd);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        group.setLayout(layout);
        group.setFont(font);
        projectText = new Text(group, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        projectText.setLayoutData(gd);
        projectText.setFont(font);
        projectText.addModifyListener(listener);
        projectButton = createPushButton(group, "Browse...", null);
        projectButton.addSelectionListener(listener);
    }

    public String getName() {
        return "Main";
    }

    public void initializeFrom(ILaunchConfiguration config) {
        try {
            useTargetProfile.setSelection(config.getAttribute(ILaunchConstants.SCREEN_SIZE_OF_TARGET, true));
            projectText.setText(config.getAttribute(ILaunchConstants.PROJECT, ""));
            widthText.setText(config.getAttribute(ILaunchConstants.SCREEN_SIZE_WIDTH, "176"));
            heightText.setText(config.getAttribute(ILaunchConstants.SCREEN_SIZE_HEIGHT, "220"));
        } catch (Exception e) {
            e.printStackTrace();
            // Ignore.
        }
    }

    public void performApply(ILaunchConfigurationWorkingCopy copy) {
        copy.setAttribute(ILaunchConstants.SCREEN_SIZE_OF_TARGET, useTargetProfile.getSelection());
        copy.setAttribute(ILaunchConstants.PROJECT, projectText.getText().trim());
        copy.setAttribute(ILaunchConstants.SCREEN_SIZE_WIDTH, widthText.getText().trim());
        copy.setAttribute(ILaunchConstants.SCREEN_SIZE_HEIGHT, heightText.getText().trim());
    }

    public void setDefaults(ILaunchConfigurationWorkingCopy arg0) {
    }

    private IProject[] getMoSyncProjects(IWorkspaceRoot root) throws CoreException {
        IProject[] allProjects = root.getProjects();
        ArrayList<IProject> result = new ArrayList<IProject>();
        for (int i = 0; i < allProjects.length; i++) {
            if (allProjects[i].isOpen() && allProjects[i].hasNature(MoSyncNature.ID)) {
                result.add(allProjects[i]);
            }
        }

        return result.toArray(new IProject[0]);
    }

    private IProject getProject() {
        return project;
    }

}
