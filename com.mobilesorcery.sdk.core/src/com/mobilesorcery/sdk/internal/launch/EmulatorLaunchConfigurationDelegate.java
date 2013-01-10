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

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.cdt.core.model.ICModelMarker;
import org.eclipse.cdt.debug.ui.CDebugUIPlugin;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate2;
import org.eclipse.debug.core.model.IPersistableSourceLocator;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.ILaunchConstants;
import com.mobilesorcery.sdk.core.MoSyncBuildJob;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncNature;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.launch.IEmulatorLauncher;
import com.mobilesorcery.sdk.core.launch.MoReLauncher;
import com.mobilesorcery.sdk.internal.BuildSession;

public class EmulatorLaunchConfigurationDelegate extends LaunchConfigurationDelegate implements ILaunchConfigurationDelegate2 {

    private static int GLOBAL_ID = 1;
    
    public static String ID = "com.mobilesorcery.launchconfigurationtype";

    private static HashMap<ILaunchConfiguration, Map<String, Object>> overriddenAttributes = new HashMap<ILaunchConfiguration, Map<String, Object>>();

    @Override
	public void launch(final ILaunchConfiguration launchConfig, final String mode, final ILaunch launch,
            final IProgressMonitor monitor) throws CoreException {
        IProject project = getProject(launchConfig);
        // We use a job just to let all current build jobs finish - but we need to spawn
        // a new thread within the job to avoid this job to block other operations that
        // we may want perform as the emulator is running.
        Job job = new Job("Launching") {
            @Override
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

    @Override
	public boolean preLaunchCheck(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
    	// Clear temporary attributes
    	clearTemporaryAttributes(configuration);

    	boolean result = true;
    	IProject project = getProject(configuration);
    	if (!shouldAutoSwitch(configuration, mode)) {
    		MoSyncProject mosyncProject = MoSyncProject.create(project);
    		IBuildConfiguration activeCfg = mosyncProject.getActiveBuildConfiguration();
    		String[] preferredTypes = getRequiredBuildConfigTypes(mode);
    		if (activeCfg == null || !activeCfg.getTypes().containsAll(Arrays.asList(preferredTypes))) {
    			result = showSwitchConfigDialog(mosyncProject, mode, activeCfg, preferredTypes);
    		}
    	}

    	IEmulatorLauncher launcher = getEmulatorLauncher(configuration, mode);
    	int tries = 0;
		while (launcher.isLaunchable(configuration, mode) == IEmulatorLauncher.REQUIRES_CONFIGURATION && tries < 2) {
			tries++;
			IEmulatorLauncher fallbackLauncher = launcher.configure(configuration, mode);
			if (fallbackLauncher == null) {
				return false;
			} else if (!fallbackLauncher.getId().equals(launcher.getId())) {
				launcher = fallbackLauncher;
				setTemporaryAttribute(configuration, ILaunchConstants.LAUNCH_DELEGATE_ID, fallbackLauncher.getId());
			}
    	}

		// And a final check in case configuration failed.
		assertLaunchable(launcher, configuration, mode);
    	return result && super.preLaunchCheck(configuration, mode, monitor);
    }

	private static void clearTemporaryAttributes(ILaunchConfiguration configuration) {
		overriddenAttributes.remove(configuration);
	}

	private static void setTemporaryAttribute(ILaunchConfiguration configuration, String attr, Object value) {
		Map<String, Object> attributes = overriddenAttributes.get(configuration);
		if (attributes == null) {
			attributes = new HashMap<String, Object>();
			overriddenAttributes.put(configuration, attributes);
		}
		attributes.put(attr, value);
	}

	private static Object getTemporaryAttribute(ILaunchConfiguration configuration, String attr) {
		Map<String, Object> attributes = overriddenAttributes.get(configuration);
		return attributes == null ? null : attributes.get(attr);
	}

	public static IEmulatorLauncher getSessionLauncher(ILaunchConfiguration config) {
		String launcherId = (String) getTemporaryAttribute(config, ILaunchConstants.LAUNCH_DELEGATE_ID);
		return CoreMoSyncPlugin.getDefault().getEmulatorLauncher(launcherId);
	}

	private void assertLaunchable(IEmulatorLauncher launcher, ILaunchConfiguration launchConfig, String mode)
			throws CoreException {
		if (launcher.isLaunchable(launchConfig, mode) != IEmulatorLauncher.LAUNCHABLE) {
			IBuildVariant variant = getVariant(launchConfig, mode);
			throw new CoreException(
					new Status(
							IStatus.ERROR,
							CoreMoSyncPlugin.PLUGIN_ID,
							MessageFormat
									.format("Cannot use {0} in execution mode \"{1}\" on platform {2}.",
											launcher.getName(), mode,
											variant.getProfile())));
		}
	}

    private boolean showSwitchConfigDialog(MoSyncProject mosyncProject, String mode,
			final IBuildConfiguration activeCfg, String[] requiredTypes) {
    	if (isDebugMode(mode)) {
    		Display d = PlatformUI.getWorkbench().getDisplay();
    		final boolean[] result = new boolean[1];
    		d.syncExec(new Runnable() {
				@Override
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
            @Override
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
        if (MoSyncBuilder.isLib(mosyncProject)) {
            throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID,
                    "Cannot execute a library; please compile as application"));
        }
        
        if (MoSyncBuilder.isExtension(mosyncProject)) {
            throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID,
                    "Cannot execute a library; please compile as application"));
        }

        launchDelegate(launchConfig, mode, launch, emulatorId, monitor);

    }

    public void launchDelegate(ILaunchConfiguration launchConfig, String mode, ILaunch launch, int emulatorId, IProgressMonitor monitor)
            throws CoreException {
    	IEmulatorLauncher launcher = getEmulatorLauncher(launchConfig, mode);
    	launcher.launch(launchConfig, mode, launch, emulatorId, monitor);
    }

    public static IEmulatorLauncher getEmulatorLauncher(ILaunchConfiguration launchConfig, String mode) throws CoreException {
    	IEmulatorLauncher launcher = getSessionLauncher(launchConfig);
    	if (launcher != null) {
    		return launcher;
    	}

    	String delegateId = getLaunchDelegateId(launchConfig, mode, true);
    	launcher = CoreMoSyncPlugin.getDefault().getEmulatorLauncher(delegateId);
    	if (launcher == null) {
    		throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, "Could not find emulator for launching."));
    	}
    	return launcher;
    }

    protected String getLaunchDelegateId(ILaunchConfiguration launchConfig, String mode) throws CoreException {
		return getLaunchDelegateId(launchConfig, mode, allowsExternalEmulators());
	}

	protected static String getLaunchDelegateId(ILaunchConfiguration launchConfig, String mode, boolean allowExternalEmulators) throws CoreException {
    	if (allowExternalEmulators) {
			String delegateId = launchConfig.getAttribute(ILaunchConstants.LAUNCH_DELEGATE_ID, MoReLauncher.ID);
			String temporaryDelegateId = (String) getTemporaryAttribute(launchConfig, ILaunchConstants.LAUNCH_DELEGATE_ID);
			return temporaryDelegateId == null ? delegateId :  temporaryDelegateId;
    	} else {
    		// Fallback is MoRe.
    		return MoReLauncher.ID;
    	}
    }

	/**
     * Returns whether this launch configuration allows other emulators
     * than the default. Clients may override.
     * @return
     */
    protected boolean allowsExternalEmulators() {
    	return true;
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
		IEmulatorLauncher emulatorLauncher = getEmulatorLauncher(launchConfig, mode);
		IBuildVariant variant = emulatorLauncher.getVariant(launchConfig, mode);
		return variant;
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

    @Override
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

    @Override
	public boolean buildForLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
        final IProject project = getProject(configuration);
        IBuildVariant variant = getVariant(configuration, mode);
        IBuildSession session = new BuildSession(Arrays.asList(variant), BuildSession.DO_SAVE_DIRTY_EDITORS | BuildSession.DO_BUILD_RESOURCES | BuildSession.DO_LINK | BuildSession.DO_PACK);

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

	public static IBuildConfiguration getAutoSwitchBuildConfiguration(ILaunchConfiguration configuration, String mode) throws CoreException {
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

	public static boolean doesConfigMatch(ILaunchConfiguration config,
			IProject project, String mode) throws CoreException {
    	boolean sameProject = config.getAttribute(ILaunchConstants.PROJECT, "").equals(project.getName());
    	if (!sameProject) {
    		return false;
    	} // else, interesting...

    	IEmulatorLauncher emulator = EmulatorLaunchConfigurationDelegate.getEmulatorLauncher(config, mode);
		return (emulator.isLaunchable(config, mode) != IEmulatorLauncher.UNLAUNCHABLE);
	}

}
