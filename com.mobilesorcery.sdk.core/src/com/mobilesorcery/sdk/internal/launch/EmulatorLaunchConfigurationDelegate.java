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
package com.mobilesorcery.sdk.internal.launch;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ICDescriptor;
import org.eclipse.cdt.core.IBinaryParser.IBinaryObject;
import org.eclipse.cdt.core.model.ICModelMarker;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.debug.core.cdi.ICDISession;
import org.eclipse.cdt.debug.ui.CDebugUIPlugin;
import org.eclipse.cdt.internal.core.model.CModelManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate2;
import org.eclipse.debug.core.model.IPersistableSourceLocator;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

import com.mobilesorcery.sdk.core.BuildVariant;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.ILaunchConstants;
import com.mobilesorcery.sdk.core.IProcessUtil;
import com.mobilesorcery.sdk.core.MoSyncBuildJob;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncNature;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.SpawnedProcess;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.launch.IEmulatorLauncher;
import com.mobilesorcery.sdk.core.launch.MoReLauncher;
import com.mobilesorcery.sdk.internal.BuildSession;
import com.mobilesorcery.sdk.internal.EmulatorOutputParser;
import com.mobilesorcery.sdk.internal.OSPipeInputStream;
import com.mobilesorcery.sdk.internal.debug.MoSyncCDebugTarget;
import com.mobilesorcery.sdk.internal.debug.MoSyncDebugger;
import com.mobilesorcery.sdk.profiles.IProfile;

public class EmulatorLaunchConfigurationDelegate extends LaunchConfigurationDelegate implements ILaunchConfigurationDelegate2 {

    private static int GLOBAL_ID = 1;

    public void launch(final ILaunchConfiguration launchConfig, final String mode, final ILaunch launch,
            final IProgressMonitor monitor) throws CoreException {
        IProject project = getProject(launchConfig);
        // We use a job just to let all current build jobs finish - but we need to spawn 
        // a new thread within the job to avoid this job to block other operations that
        // we may want perform as the emulator is running.
        Job job = new Job("Launching") {
            public IStatus run(IProgressMonitor monitor) {
                final int emulatorId = getNextId();
                launchAsync(launchConfig, mode, launch, emulatorId, monitor);
                return Status.OK_STATUS;
            }
        };
        
        job.setRule(project);
        job.setSystem(true);
        job.schedule();
    }

    public boolean preLaunchCheck(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
    	IProject project = getProject(configuration);
    	if (!shouldAutoSwitch(configuration, mode)) {
    		MoSyncProject mosyncProject = MoSyncProject.create(project);
    		IBuildConfiguration activeCfg = mosyncProject.getActiveBuildConfiguration();
    		String[] preferredTypes = getRequiredBuildConfigTypes(mode);
    		if (activeCfg == null || !activeCfg.getTypes().containsAll(Arrays.asList(preferredTypes))) {
    			return showSwitchConfigDialog(mosyncProject, mode, activeCfg, preferredTypes);
    		}
    	}
    	
    	return super.preLaunchCheck(configuration, mode, monitor);
    }
    
    private boolean showSwitchConfigDialog(MoSyncProject mosyncProject, String mode,
			final IBuildConfiguration activeCfg, String[] requiredTypes) {
    	if (isDebugMode(mode)) {
    		Display d = PlatformUI.getWorkbench().getDisplay();
    		final boolean[] result = new boolean[1];
    		d.syncExec(new Runnable() {
				public void run() {
			    	Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
					MessageDialog dialog = new MessageDialog(shell, "Incompatible build configuration", null,
						MessageFormat.format("The build configuration \"{0}\" is not intended for debugging. Debug anyway?",
						activeCfg.getId()), MessageDialog.WARNING, new String[] { "Debug", "Cancel" }, 1);
					result[0] = dialog.open() == 0;
				}    			
    		});
			return result[0];
    	}
		return true;
	}

	/**
     * Clients may override. This method returns the set of required
     * build configuration types for this launch. (Used to determine
     * whether to show a dialog to user).
     * @param mode
     * @return
     */
    protected String[] getRequiredBuildConfigTypes(String mode) {
		return new String[] { isDebugMode(mode) ? IBuildConfiguration.DEBUG_TYPE : IBuildConfiguration.RELEASE_TYPE };
	}

	/**
     * Returns the default build configuration to use for a given mode (debug or launch).
     * @param mode
     * @return
     */
    public static String getDefaultBuildConfiguration(MoSyncProject project, String mode) {
        String type = "debug".equals(mode) ? IBuildConfiguration.DEBUG_TYPE : IBuildConfiguration.RELEASE_TYPE;
        SortedSet<String> candidateCfgs = project == null ? new TreeSet<String>() : project.getBuildConfigurationsOfType(type);
        if (candidateCfgs.isEmpty()) {
            return "debug".equals(mode) ? IBuildConfiguration.DEBUG_ID : IBuildConfiguration.RELEASE_ID;
        } else {
            return candidateCfgs.first();
        }
    }
    
    public void launchAsync(final ILaunchConfiguration launchConfig, final String mode, final ILaunch launch, final int emulatorId, final IProgressMonitor monitor) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    launchSync(launchConfig, mode, launch, emulatorId, monitor);
                } catch (CoreException e) {
                    StatusManager.getManager().handle(e.getStatus(), StatusManager.SHOW);
                }
            }
        }, MessageFormat.format("Emulator {0}", emulatorId)); 
        
        t.setDaemon(true);
        t.start();
    }
    
    public void launchSync(ILaunchConfiguration launchConfig, String mode, ILaunch launch, int emulatorId, IProgressMonitor monitor)
    throws CoreException {
        IProject project = getProject(launchConfig);
        IBuildVariant variant = getVariant(launchConfig, mode);
        
        if (!MoSyncNature.hasNature(project) && MoSyncNature.isCompatible(project)) {
        	throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, MessageFormat.format(
        			"Could not launch ''{0}'' - please upgrade this project to the new MoSync project type (available in the context menu)", project.getName())));
        }

        if (project.findMaxProblemSeverity(ICModelMarker.C_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE) == IMarker.SEVERITY_ERROR) {
            throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, MessageFormat.format("Could not launch; build errors in project {0}", project.getName())));
        }
        
        if (!getLaunchDir(MoSyncProject.create(project), variant).toFile().exists()) {
            throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, "Could not find build directory - please make sure your project is built"));
        }
        
        MoSyncProject mosyncProject = MoSyncProject.create(project);
        if (MoSyncBuilder.PROJECT_TYPE_LIBRARY.equals(mosyncProject.getProperty(MoSyncBuilder.PROJECT_TYPE))) {
            throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID,
                    "Cannot execute a library; please compile as application"));
        }
        
        launchDelegate(launchConfig, mode, launch, emulatorId, monitor);
        
    }

    public void launchDelegate(ILaunchConfiguration launchConfig, String mode, ILaunch launch, int emulatorId, IProgressMonitor monitor)
            throws CoreException {
    	String delegateId = getLaunchDelegateId(launchConfig);
    	IEmulatorLauncher launcher = CoreMoSyncPlugin.getDefault().getEmulatorLauncher(delegateId);
    	launcher.launch(launchConfig, mode, launch, emulatorId, monitor);
    }
    
    protected String getLaunchDelegateId(ILaunchConfiguration launchConfig) throws CoreException {
    	String delegateId = launchConfig.getAttribute(ILaunchConstants.LAUNCH_DELEGATE_ID, MoReLauncher.ID);
    	return delegateId;
    }
    
	/**
     * Returns the launch directory of this launch.
     * @param project
     * @return
     */
    public static IPath getLaunchDir(MoSyncProject project, IBuildVariant variant) {
        return MoSyncBuilder.getOutputPath(project.getWrappedProject(), variant);
    }
    
    public static IBuildVariant getVariant(ILaunchConfiguration launchConfig, String mode) throws CoreException {
		IProject project = getProject(launchConfig);
		MoSyncProject mosyncProject = MoSyncProject.create(project);
		IBuildConfiguration cfg = getAutoSwitchBuildConfiguration(launchConfig, mode);
		return new BuildVariant(mosyncProject.getTargetProfile(), cfg, false);
	}

	public static IProject getProject(ILaunchConfiguration launchConfig) throws CoreException {
		return MoSyncBuilder.getProject(launchConfig);
    }
	
    private int getNextId() {
        synchronized (EmulatorLaunchConfigurationDelegate.class) {
            GLOBAL_ID %= 256;
            return GLOBAL_ID++;
        }
    }

    public ILaunch getLaunch(ILaunchConfiguration configuration, String mode) throws CoreException {
    	//ISourceLookupDirector commonSourceLookupDirector = CDebugCorePlugin.getDefault().getCommonSourceLookupDirector();
    	
    	Launch launch = new Launch(configuration, mode, null);
    	// We implement this ourselves so we can add source lookup (and hence niceties like 
        // clicking on stack trace -> open editor
        setDefaultSourceLocator(launch, configuration);
    	return launch;
    }
    
    protected void setDefaultSourceLocator(ILaunch launch, ILaunchConfiguration configuration) throws CoreException {
        if (launch.getSourceLocator() == null) {
            IPersistableSourceLocator sourceLocator;
            String id = configuration.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, (String)null);
            if (id == null) {
                sourceLocator = CDebugUIPlugin.createDefaultSourceLocator();
                if (sourceLocator instanceof AbstractSourceLookupDirector) {
                    ((AbstractSourceLookupDirector)sourceLocator).setId(CDebugUIPlugin.getDefaultSourceLocatorID());
                }
                sourceLocator.initializeDefaults(configuration);
           } else {
                sourceLocator = DebugPlugin.getDefault().getLaunchManager().newSourceLocator(id);
                String memento = configuration.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO, (String)null);
                if (memento == null) {
                    sourceLocator.initializeDefaults(configuration);
                } else {
                    sourceLocator.initializeFromMemento(memento);
                }
            }
            launch.setSourceLocator(sourceLocator);
        }
    }
    
    public static void configureLaunchConfigForSourceLookup(ILaunchConfigurationWorkingCopy wc) {
        wc.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, CDebugUIPlugin.getDefaultSourceLocatorID());
    }
    
    public boolean buildForLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
        final IProject project = getProject(configuration);
        IBuildVariant variant = getVariant(configuration, mode);
        IBuildSession session = new BuildSession(Arrays.asList(variant), BuildSession.DO_BUILD_RESOURCES | BuildSession.DO_LINK);
        
		// No dialogs should pop up.
        Job job = new MoSyncBuildJob(MoSyncProject.create(project), session, variant);
        job.setName("Prelaunch build");
        job.schedule();
        return false;
    }
    
    protected static boolean shouldAutoSwitch(ILaunchConfiguration configuration, String mode) throws CoreException {
    	String autoChangeConfigKey = isDebugMode(mode) ? ILaunchConstants.AUTO_CHANGE_CONFIG_DEBUG : ILaunchConstants.AUTO_CHANGE_CONFIG;
        return configuration.getAttribute(autoChangeConfigKey, false);
    }
    
	protected static IBuildConfiguration getAutoSwitchBuildConfiguration(ILaunchConfiguration configuration, String mode) throws CoreException {
        IProject project = getProject(configuration);
        MoSyncProject mosyncProject = MoSyncProject.create(project);
        // We'll let non-mosync projects slip through; they'll be handled in launchSync
        if (mosyncProject != null && mosyncProject.areBuildConfigurationsSupported()) {
            
            String buildConfigKey = isDebugMode(mode) ? ILaunchConstants.BUILD_CONFIG_DEBUG : ILaunchConstants.BUILD_CONFIG;

            if (shouldAutoSwitch(configuration, mode)) {
                String buildConfig = configuration.getAttribute(buildConfigKey, getDefaultBuildConfiguration(mosyncProject, mode));
                IBuildConfiguration activeBuildConfig = mosyncProject.getActiveBuildConfiguration();
                String activeBuildConfigId = activeBuildConfig == null ? null : activeBuildConfig.getId();
                if (buildConfig != null && !buildConfig.equals(activeBuildConfigId) && mosyncProject.getBuildConfiguration(buildConfig) != null) {
                    return mosyncProject.getBuildConfiguration(buildConfig);
                }
            }
        }
        
        return mosyncProject.getActiveBuildConfiguration();
    }
        
    public static boolean isDebugMode(String mode) {
        boolean isDebugMode = "debug".equals(mode);
        return isDebugMode;
	}

}
