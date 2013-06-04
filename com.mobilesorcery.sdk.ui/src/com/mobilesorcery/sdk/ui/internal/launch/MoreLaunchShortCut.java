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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.ILaunchConstants;
import com.mobilesorcery.sdk.core.launch.AutomaticEmulatorLauncher;
import com.mobilesorcery.sdk.core.launch.IEmulatorLauncher;
import com.mobilesorcery.sdk.internal.launch.EmulatorLaunchConfigurationDelegate;

public class MoreLaunchShortCut implements ILaunchShortcut2 {

    @Override
	public void launch(ISelection selection, String mode) {
        launch(getProjectFromSelection(selection), mode);
    }

    private IProject getProjectFromSelection(ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            return getProjectFromSelection(((IStructuredSelection) selection).toArray());
        }

        return null;
    }

    private IProject getProjectFromSelection(Object[] selectionArray) {
        IProject project = null;
        for (int i = 0; project == null && i < selectionArray.length; i++) {
            Object selection = selectionArray[i];
            if (selection instanceof IEditorPart) {
                selection = ((IEditorPart) selection).getEditorInput();
            }

            if (selection instanceof IAdaptable) {
                selection = ((IAdaptable) selection).getAdapter(IResource.class);
            }

            if (selection instanceof IResource) {
                project = ((IResource) selection).getProject();
            }
        }

        return project;
    }

    private void launch(IProject project, String mode) {
        if (project != null) {
            ILaunchConfiguration config = findLaunchConfiguration(project, mode);
            if (config != null) {
                try {
                    DebugUITools.buildAndLaunch(config, mode, new NullProgressMonitor());
                } catch (CoreException e) {
                    ErrorDialog.openError(getShell(), "Error Launching " + config.getName(), e.getMessage(), e.getStatus());
                }
            }
        }
    }

    @Override
	public void launch(IEditorPart part, String mode) {
        launch(getProjectFromSelection(new Object[] { part }), mode);
    }

    private ILaunchConfiguration findLaunchConfiguration(IProject project, String mode) {
        ILaunchConfiguration configuration = null;
        List candidateConfigs = getCandidateConfigs(project, mode);

        int candidateCount = candidateConfigs.size();
        if (candidateCount < 1) {
            configuration = createConfiguration(project, mode);
        } else if (candidateCount == 1) {
            configuration = (ILaunchConfiguration) candidateConfigs.get(0);
        } else {
            ILaunchConfiguration config = chooseConfiguration(candidateConfigs, mode);
            if (config != null) {
                configuration = config;
            }
        }

        return configuration;
    }

    private static ILaunchConfigurationType getMoreConfigType() {
    	 ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
         ILaunchConfigurationType configType = lm.getLaunchConfigurationType(EmulatorLaunchConfigurationDelegate.ID);
         return configType;
    }
    
    protected ILaunchConfigurationType getConfigType() {
       return getMoreConfigType();
    }

    /**
     * Clients should override.
     * @return
     */
    protected String getPreferredLauncherId() {
    	return AutomaticEmulatorLauncher.ID;
    }

    public ILaunchConfiguration createConfiguration(IProject project, String mode) {
    	return createConfiguration(project, mode, getPreferredLauncherId(), false, false);
    }
    
    public static ILaunchConfiguration createConfiguration(IProject project, String mode, String launcherId, boolean onDevice, boolean privateLaunch) {
        ILaunchConfiguration config = null;
        try {
            ILaunchConfigurationType configType = getMoreConfigType();

            String launchConfigName = DebugPlugin.getDefault().getLaunchManager().generateLaunchConfigurationName(project.getName());
            ILaunchConfigurationWorkingCopy wc = configType.newInstance(null, launchConfigName);

            if ("run".equals(mode)) {
                wc.setAttribute(DebugPlugin.ATTR_PROCESS_FACTORY_ID, "com.mobilesorcery.sdk.builder.nonterminableprocessfactory");
            }
            DebugUITools.setLaunchPerspective(configType, ILaunchManager.RUN_MODE, IDebugUIConstants.PERSPECTIVE_DEFAULT);
            DebugUITools.setLaunchPerspective(configType, ILaunchManager.DEBUG_MODE, IDebugUIConstants.PERSPECTIVE_DEFAULT);

            EmulatorLaunchConfigurationDelegate.configureLaunchConfigForSourceLookup(wc);

            wc.setAttribute(ILaunchConstants.PROJECT, project.getName());

            // Let the launcher add its own attributes
            IEmulatorLauncher launcher = CoreMoSyncPlugin.getDefault().getEmulatorLauncher(launcherId);
            wc.setAttribute(ILaunchConstants.LAUNCH_DELEGATE_ID, launcherId);
            launcher.setDefaultAttributes(wc);

            if (privateLaunch) {
            	wc.setAttribute(IDebugUIConstants.ATTR_PRIVATE, true);
            }
            if (onDevice) {
            	wc.setAttribute(ILaunchConstants.ON_DEVICE, true);
            }
            config = wc.doSave();
        } catch (CoreException ce) {
            CoreMoSyncPlugin.getDefault().getLog().log(
                    new Status(IStatus.WARNING, CoreMoSyncPlugin.PLUGIN_ID, "Could not create launch configuration", ce));
        }

        return config;
    }

    /**
     * Returns the configuration to launch with (or null if none, in which case a default configuration will be selected)
     * @return
     */
    protected String getInitialConfigurationId(IProject project, String mode) {
        return null;
    }

    private List<ILaunchConfiguration> getCandidateConfigs(IProject project, String mode) {
        ILaunchConfigurationType configType = getConfigType();

        List<ILaunchConfiguration> candidateConfigs = Collections.EMPTY_LIST;
        if (project != null) {
            try {
                ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(configType);
                candidateConfigs = new ArrayList<ILaunchConfiguration>(configs.length);
                for (int i = 0; i < configs.length; i++) {
                    ILaunchConfiguration config = configs[i];
                    if (doesConfigMatch(config, project, mode)) {
                        candidateConfigs.add(config);
                    }
                }
            } catch (CoreException ce) {
                CoreMoSyncPlugin.getDefault().getLog().log(
                        new Status(IStatus.WARNING, CoreMoSyncPlugin.PLUGIN_ID, "Could not find launch configurations", ce));
            }
        }

        return candidateConfigs;
    }

    private boolean doesConfigMatch(ILaunchConfiguration config, IProject project, String mode) throws CoreException {
    	return EmulatorLaunchConfigurationDelegate.doesConfigMatch(config, project, mode);
	}

	private ILaunchConfiguration chooseConfiguration(List configList, String mode) {
        IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
        ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), labelProvider);
        dialog.setElements(configList.toArray());
        dialog.setTitle("MoSync Emulator launch configuration");
        dialog.setMessage("Select launch configuration");
        dialog.setMultipleSelection(false);

        int result = dialog.open();
        labelProvider.dispose();
        if (result == ElementListSelectionDialog.OK) {
            return (ILaunchConfiguration) dialog.getFirstResult();
        }
        return null;
    }

    private Shell getShell() {
        Shell shell = null;

        IWorkbenchWindow workbenchWindow = CoreMoSyncPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
        if (workbenchWindow != null) {
            shell = workbenchWindow.getShell();
        }

        return shell;
    }

    // ILaunchShortcut2
    @Override
	public ILaunchConfiguration[] getLaunchConfigurations(ISelection selection) {
        return getCandidateConfigs(getProjectFromSelection(selection), "run").toArray(new ILaunchConfiguration[0]);
    }

    @Override
	public ILaunchConfiguration[] getLaunchConfigurations(IEditorPart editorpart) {
        return getCandidateConfigs(getProjectFromSelection(new Object[] { editorpart }), "run").toArray(new ILaunchConfiguration[0]);
    }

    @Override
	public IResource getLaunchableResource(ISelection selection) {
        return getProjectFromSelection(selection);
    }

    @Override
	public IResource getLaunchableResource(IEditorPart editorpart) {
        return getProjectFromSelection(new Object[] { editorpart });
    }

}
