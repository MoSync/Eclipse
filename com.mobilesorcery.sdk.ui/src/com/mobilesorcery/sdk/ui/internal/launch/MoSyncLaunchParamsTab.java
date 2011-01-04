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
package com.mobilesorcery.sdk.ui.internal.launch;

import java.util.HashMap;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IEmulatorProcessListener;
import com.mobilesorcery.sdk.core.ILaunchConstants;
import com.mobilesorcery.sdk.core.MoSyncNature;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.launch.IEmulatorLauncher;
import com.mobilesorcery.sdk.core.launch.MoReLauncher;
import com.mobilesorcery.sdk.internal.launch.EmulatorLaunchConfigurationDelegate;
import com.mobilesorcery.sdk.ui.BuildConfigurationsContentProvider;
import com.mobilesorcery.sdk.ui.BuildConfigurationsLabelProvider;
import com.mobilesorcery.sdk.ui.MoSyncProjectSelectionDialog;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.launch.IEmulatorLaunchConfigurationPart;

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

	private class UpdateConfigurationListener implements ModifyListener {
		public void modifyText(ModifyEvent e) {
			updateConfigurations();
		}
	}

	private Text projectText;
	private Button projectButton;
	private TabListener listener = new TabListener();
	private IProject project;
	private String mode;
	private Button changeConfiguration;
	private ComboViewer configurations;
	private Group configurationGroup;
	private String[] debugBuildConfigurationTypes = new String[] { IBuildConfiguration.DEBUG_TYPE };
	private String[] buildConfigurationTypes = new String[] { IBuildConfiguration.RELEASE_TYPE };
	private ComboViewer launchDelegateList;
	private Composite launchDelegateHolder;
	private StackLayout launchDelegateHolderLayout;
	private HashMap<String, Composite> delegateComposites = new HashMap<String, Composite>();
	private ILaunchConfiguration config;
	private HashMap<String, IEmulatorLaunchConfigurationPart> launcherParts = new HashMap<String, IEmulatorLaunchConfigurationPart>();

	public void createControl(Composite parent) {
		Composite control = new Composite(parent, SWT.NONE);
		control.setLayout(new GridLayout(1, false));
		createProjectEditor(control);
		createLaunchDelegateEditor(control);
		createConfigurationEditor(control);
		setControl(control);
	}

	private void createLaunchDelegateEditor(Composite control) {
		Set<String> ids = CoreMoSyncPlugin.getDefault()
				.getEmulatorLauncherIds();
		Composite launchDelegateHolderParent = control;
		
		if (ids.size() > 1) {
			Group launchDelegateGroup = new Group(control, SWT.NONE);
			launchDelegateGroup.setText("&Emulator");
			launchDelegateGroup.setLayout(new GridLayout(1, false));
			launchDelegateList = new ComboViewer(launchDelegateGroup);
			launchDelegateList.setContentProvider(new ArrayContentProvider());
			launchDelegateList.setInput(ids.toArray());
			launchDelegateList.setLabelProvider(new LabelProvider() {
				public String getText(Object element) {
					return CoreMoSyncPlugin.getDefault()
							.getEmulatorLauncher((String) element).getName();
				}
			});
			launchDelegateList
					.addSelectionChangedListener(new ISelectionChangedListener() {
						@Override
						public void selectionChanged(SelectionChangedEvent event) {
							IStructuredSelection selection = (IStructuredSelection) event
									.getSelection();
							String id = (String) selection.getFirstElement();
							switchDelegate(id);
						}
					});
			
			launchDelegateList.setSelection(new StructuredSelection(
					MoReLauncher.ID));
			launchDelegateHolderParent = launchDelegateGroup;
		} 
		
		launchDelegateHolder = new Composite(launchDelegateHolderParent, SWT.NONE);
		launchDelegateHolder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		launchDelegateHolderLayout = new StackLayout();
		launchDelegateHolder.setLayout(launchDelegateHolderLayout);
	}

	protected void switchDelegate(String id) {
		Composite delegateComposite = delegateComposites.get(id);
		if (delegateComposite == null) {
			delegateComposite = createDelegateComposite(id,
					launchDelegateHolder);
		}

		launchDelegateHolderLayout.topControl = delegateComposite;
		launchDelegateHolder.layout();
	}

	private Composite createDelegateComposite(String id, Composite parent) {
		IEmulatorLaunchConfigurationPart launcherPart = MosyncUIPlugin
				.getDefault().getEmulatorLauncherPart(id);
		launcherParts.put(id, launcherPart);
		Composite control = launcherPart.createControl(parent);
		try {
			launcherPart.init(config);
		} catch (CoreException e) {
			CoreMoSyncPlugin.getDefault().log(e);
		}
		return control;
	}

	private IProject selectProject() {
		MoSyncProjectSelectionDialog dialog = new MoSyncProjectSelectionDialog(
				getShell());
		dialog.setInitialProject(getProject());
		return dialog.selectProject();
	}

	private void createConfigurationEditor(Composite control) {
		configurationGroup = new Group(control, SWT.NONE);
		configurationGroup
				.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		configurationGroup.setText("&Configuration");
		configurationGroup.setLayout(new GridLayout(1, false));

		changeConfiguration = new Button(configurationGroup, SWT.CHECK);
		changeConfiguration
				.setText(isDebugMode() ? "&Automatically switch to this configuration before debugging"
						: "&Automatically switch to this configuration before launching");
		changeConfiguration.addSelectionListener(listener);

		configurations = new ComboViewer(configurationGroup, SWT.READ_ONLY
				| SWT.BORDER);
		configurations.getCombo().addSelectionListener(listener);
	}

	private void updateConfigurations() {
		MoSyncProject project = selectedProject();
		if (project != null) {
			configurations
					.setContentProvider(new BuildConfigurationsContentProvider(
							project,
							isDebugMode() ? debugBuildConfigurationTypes
									: buildConfigurationTypes));
			configurations
					.setLabelProvider(new BuildConfigurationsLabelProvider(
							project));
			configurations.setInput(project);
		}
	}

	public void setBuildConfigurationTypes(boolean isDebug, String... types) {
		if (isDebug) {
			debugBuildConfigurationTypes = types;
		} else {
			buildConfigurationTypes = types;
		}
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
		projectText.addModifyListener(new UpdateConfigurationListener());
		projectButton = createPushButton(group, "Browse...", null);
		projectButton.addSelectionListener(listener);
	}

	public String getName() {
		return "Main";
	}

	public void initializeFrom(ILaunchConfiguration config) {
		try {
			this.config = config;
			projectText.setText(config.getAttribute(ILaunchConstants.PROJECT,
					""));
			initializeBuildConfigurationOptions(config);
			updateLaunchConfigurationDialog();
			switchDelegate(config.getAttribute(ILaunchConstants.LAUNCH_DELEGATE_ID, MoReLauncher.ID));
		} catch (Exception e) {
			e.printStackTrace();
			// Ignore.
		}
	}

	public void initializeBuildConfigurationOptions(ILaunchConfiguration config)
			throws CoreException {
		String defaultBuildConfig = EmulatorLaunchConfigurationDelegate
				.getDefaultBuildConfiguration(
						MoSyncProject.create(getProject()), mode);
		changeConfiguration.setSelection(config.getAttribute(
				getAutoChangeConfigKey(), false));
		String buildConfiguration = config.getAttribute(getBuildConfigKey(),
				defaultBuildConfig);
		configurations
				.setSelection(new StructuredSelection(buildConfiguration));
	}

	private boolean isDebugMode() {
		return "debug".equals(mode);
	}

	private String getAutoChangeConfigKey() {
		return isDebugMode() ? ILaunchConstants.AUTO_CHANGE_CONFIG_DEBUG
				: ILaunchConstants.AUTO_CHANGE_CONFIG;
	}

	private String getBuildConfigKey() {
		return isDebugMode() ? ILaunchConstants.BUILD_CONFIG_DEBUG
				: ILaunchConstants.BUILD_CONFIG;
	}
	
	private String getCurrentLauncherDelegateId() {
		String delegateId = MoReLauncher.ID;
		if (launchDelegateList != null) {
			IStructuredSelection selection = (IStructuredSelection) launchDelegateList.getSelection();
			delegateId = (String) selection.getFirstElement();
		}
		
		return delegateId;
	}

	public void performApply(ILaunchConfigurationWorkingCopy copy) {
		for (IEmulatorLaunchConfigurationPart launcherPart : launcherParts
				.values()) {
			launcherPart.apply(copy);
		}
		copy.setAttribute(ILaunchConstants.PROJECT, projectText.getText()
				.trim());
		copy.setAttribute(getAutoChangeConfigKey(),
				changeConfiguration.getSelection());
		copy.setAttribute(getBuildConfigKey(), getSelectedBuildConfiguration());
		copy.setAttribute(ILaunchConstants.LAUNCH_DELEGATE_ID, getCurrentLauncherDelegateId());
	}

	public String getSelectedBuildConfiguration() {
		String result = (String) ((IStructuredSelection) configurations
				.getSelection()).getFirstElement();
		return result == null ? "" : result;
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy wc) {
		EmulatorLaunchConfigurationDelegate
				.configureLaunchConfigForSourceLookup(wc);
	}

	private IProject getProject() {
		return project;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public void updateLaunchConfigurationDialog() {
		MoSyncProject project = selectedProject();
		boolean configurationsVisible = project != null
				&& project.areBuildConfigurationsSupported();
		boolean comboEnabled = changeConfiguration.getSelection()
				&& configurationsVisible;
		configurations.getControl().setEnabled(comboEnabled);
		configurationGroup.setVisible(configurationsVisible);

		super.updateLaunchConfigurationDialog();
	}

	private MoSyncProject selectedProject() {
		try {
			IProject project = getWorkspaceRoot().getProject(
					projectText.getText().trim());
			if (project != null && project.exists() && project.isOpen()
					&& project.hasNature(MoSyncNature.ID)) {
				return MoSyncProject.create(project);
			}
		} catch (Exception e) {
			// Ignore.
		}

		return null;
	}
}
