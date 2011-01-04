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
package com.mobilesorcery.sdk.core;

import java.io.FileInputStream;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.core.launch.IEmulatorLauncher;
import com.mobilesorcery.sdk.core.launch.MoReLauncher;
import com.mobilesorcery.sdk.core.security.IApplicationPermissions;
import com.mobilesorcery.sdk.internal.ErrorPackager;
import com.mobilesorcery.sdk.internal.PID;
import com.mobilesorcery.sdk.internal.PROCESS;
import com.mobilesorcery.sdk.internal.PackagerProxy;
import com.mobilesorcery.sdk.internal.PropertyInitializerProxy;
import com.mobilesorcery.sdk.internal.RebuildListener;
import com.mobilesorcery.sdk.internal.ReindexListener;
import com.mobilesorcery.sdk.internal.debug.MoSyncBreakpointSynchronizer;
import com.mobilesorcery.sdk.internal.dependencies.DependencyManager;
import com.mobilesorcery.sdk.internal.launch.EmulatorLauncherProxy;
import com.mobilesorcery.sdk.internal.security.ApplicationPermissions;
import com.mobilesorcery.sdk.lib.JNALibInitializer;
import com.mobilesorcery.sdk.profiles.filter.DeviceFilterFactoryProxy;
import com.mobilesorcery.sdk.profiles.filter.IDeviceFilterFactory;
import com.mobilesorcery.sdk.profiles.filter.elementfactories.ConstantFilterFactory;
import com.mobilesorcery.sdk.profiles.filter.elementfactories.FeatureFilterFactory;
import com.mobilesorcery.sdk.profiles.filter.elementfactories.ProfileFilterFactory;
import com.mobilesorcery.sdk.profiles.filter.elementfactories.VendorFilterFactory;

/**
 * The activator class controls the plug-in life cycle
 */
public class CoreMoSyncPlugin extends AbstractUIPlugin implements IPropertyChangeListener, IResourceChangeListener {
	
    // The plug-in ID
    public static final String PLUGIN_ID = "com.mobilesorcery.sdk.core";

    // The shared instance
    private static CoreMoSyncPlugin plugin;

    private ArrayList<Pattern> patterns = new ArrayList<Pattern>();

    private ArrayList<IPackager> packagers = new ArrayList<IPackager>();

    private Map<String, Map<String, IPropertyInitializer>> propertyInitializers = new HashMap<String, Map<String, IPropertyInitializer>>();

	private HashMap<String, IEmulatorLauncher> launchers = new HashMap<String, IEmulatorLauncher>();

	private DependencyManager<IProject> projectDependencyManager;

    private Properties panicMessages = new Properties();

    private ReindexListener reindexListener;

    private Integer[] sortedPanicErrorCodes;

	private MoSyncBreakpointSynchronizer bpSync;
	
	private boolean isHeadless = false;

	private HashMap<String, IDeviceFilterFactory> factories;

	private boolean updaterInitialized;

	private IUpdater updater;

	private IProvider<IProcessConsole, String> ideProcessConsoleProvider;

	private EmulatorProcessManager emulatorProcessManager;

    private String[] buildConfigurationTypes;

    private HashMap<String, Integer> logCounts = new HashMap<String, Integer>();

    /**
     * The constructor
     */
    public CoreMoSyncPlugin() {
    }

    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        isHeadless = Boolean.TRUE.equals(System.getProperty("com.mobilesorcery.headless"));
        initReIndexerListener();
        initRebuildListener();
        initNativeLibs(context);
        initPackagers();
        initDeviceFilterFactories();
        initPanicErrorMessages();
        initPropertyInitializers();
        initGlobalDependencyManager();        
        initEmulatorProcessManager();
        installBreakpointHack();
        installResourceListener();
        initBuildConfigurationTypes();
        initLaunchers();
		getPreferenceStore().addPropertyChangeListener(this); 
		initializeOnSeparateThread();
    }
    
    private void initLaunchers() {
    	IConfigurationElement[] launchers = Platform.getExtensionRegistry().getConfigurationElementsFor(IEmulatorLauncher.EXTENSION_POINT_ID);
    	for (int i = 0; i < launchers.length; i++) {
    		IConfigurationElement launcher = launchers[i];
    		String id = launcher.getAttribute("id");
    		this.launchers.put(id, new EmulatorLauncherProxy(launcher));
    	}
    	// Default always present
    	this.launchers.put(MoReLauncher.ID, new MoReLauncher());
 	}

	private void initBuildConfigurationTypes() {
        IConfigurationElement[] types = Platform.getExtensionRegistry().getConfigurationElementsFor(
                BuildConfiguration.TYPE_EXTENSION_POINT);
        ArrayList<String> buildConfigurationTypes = new ArrayList<String>();
        
        // Add defaults
        buildConfigurationTypes.add(IBuildConfiguration.RELEASE_TYPE);
        buildConfigurationTypes.add(IBuildConfiguration.DEBUG_TYPE);
        
        // Add extensions
        for (int i = 0; i < types.length; i++) {
            String typeId = types[i].getAttribute("id");
            if (typeId != null) {
                buildConfigurationTypes.add(typeId);
            }
        }

        this.buildConfigurationTypes = buildConfigurationTypes.toArray(new String[0]);
    }

    void initializeOnSeparateThread() {
    	// I think we should move this to the UI plugin!
    	Thread initializerThread = new Thread(new Runnable() {
			public void run() {
				checkAutoUpdate();
			}    		
    	}, "Initializer");
    	
    	initializerThread.setDaemon(true);
    	initializerThread.start();
    }

	/**
     * Returns whether this app is running in headless mode.
     * @return
     */
    public static boolean isHeadless() {
    	return plugin.isHeadless;
    }

    /**
     * Sets this app to headless/non-headless mode.
     * Please note that this will trigger a bundle activation,
     * so if you want to make sure headless is set before that
     * use <code>System.setProperty("com.mobilesorcery.headless", true")</code>
     * @param isHeadless
     */
	public static void setHeadless(boolean isHeadless) {
		plugin.isHeadless = isHeadless;
	}

    private void installBreakpointHack() {    	
    	bpSync = new MoSyncBreakpointSynchronizer();
    	bpSync.install();
	}

	private void initGlobalDependencyManager() {
    	// Currently, all workspaces share this guy -- fixme later.
        this.projectDependencyManager = new DependencyManager<IProject>();
	}

	private void initRebuildListener() {
        MoSyncProject.addGlobalPropertyChangeListener(new RebuildListener());
    }

    public void stop(BundleContext context) throws Exception {
        plugin = null;
        projectDependencyManager = null;
        disposeUpdater();
        MoSyncProject.removeGlobalPropertyChangeListener(reindexListener);
        bpSync.uninstall();
        deinstallResourceListener();
        super.stop(context);
    }

    private void initPropertyInitializers() {
        IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(
                IPropertyInitializerDelegate.EXTENSION_POINT);
        propertyInitializers = new HashMap<String, Map<String, IPropertyInitializer>>();

        for (int i = 0; i < elements.length; i++) {
            String context = PropertyInitializerProxy.getContext(elements[i]);
            String prefix = PropertyInitializerProxy.getPrefix(elements[i]);

            if (context != null && prefix != null) {
                Map<String, IPropertyInitializer> prefixMap = propertyInitializers.get(context);
                if (prefixMap == null) {
                    prefixMap = new HashMap<String, IPropertyInitializer>();
                    propertyInitializers.put(context, prefixMap);
                }

                prefixMap.put(prefix, new PropertyInitializerProxy(elements[i]));
            }
        }
    }

    /**
     * <p>
     * From the registered <code>IPropertyInitializerDelegate</code>s, returns
     * the default value for <code>key</code>, where <code>key</code> has the
     * format <code>prefix:subkey</code>.
     * </p>
     * <p>
     * <code>IPropertyInitializerDelegate</code>s are always registered with
     * context and prefix, which are used for lookup.
     * </p>
     * <p>
     * The context is the same as is returned from the <code>getContext()</code>
     * method in <code>IPropertyOwner</code>.
     *
     * @param owner
     * @param key
     * @return May return <code>null</code>
     */
    public String getDefaultValue(IPropertyOwner owner, String key) {
        Map<String, IPropertyInitializer> prefixMap = propertyInitializers.get(owner.getContext());
        if (prefixMap != null) {
            String[] prefixAndSubkey = key.split(":", 2);
            if (prefixAndSubkey.length == 2) {
                IPropertyInitializer initializer = prefixMap.get(prefixAndSubkey[0]);
                if (initializer != null) {
                	return initializer.getDefaultValue(owner, key);
                }
            }
        }

        return null;
    }

    private void initReIndexerListener() {
        reindexListener = new ReindexListener();
        MoSyncProject.addGlobalPropertyChangeListener(new ReindexListener());
    }

    private void initEmulatorProcessManager() {
    	this.emulatorProcessManager = new EmulatorProcessManager();
	}

    private void initPanicErrorMessages() {
        try {
        	panicMessages = new Properties();

        	InputStream messagesStream = 
        		new FileInputStream(MoSyncTool.getDefault().getMoSyncHome().
        				append("eclipse/paniccodes.properties").toFile());
        	
        	try {
        		panicMessages.load(messagesStream);
           } finally {
        		Util.safeClose(messagesStream);
        	}
        } catch (Exception e) {
            // Just ignore.
            getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, "Could not initialize panic messages", e));
        }

        TreeSet<Integer> result = new TreeSet<Integer>();
        for (Enumeration errorCodes = panicMessages.keys(); errorCodes.hasMoreElements(); ) {
            try {
                String errorCode = (String) errorCodes.nextElement();
                int errorCodeValue = Integer.parseInt(errorCode);
                result.add(errorCodeValue);
            } catch (Exception e) {
                // Just ignore.
            }
        }

        sortedPanicErrorCodes = result.toArray(new Integer[result.size()]);
    }


    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static CoreMoSyncPlugin getDefault() {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in
     * relative path
     *
     * @param path
     *            the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    public void log(Throwable e) {
        getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
    }

    private void initNativeLibs(BundleContext context) {
        try {	
            JNALibInitializer.init(this.getBundle(), "libpipe");
            @SuppressWarnings("unused")
			PROCESS dummy = PROCESS.INSTANCE; // Just to execute the .clinit.

            JNALibInitializer.init(this.getBundle(), "libpid2");

            if (isDebugging()) {
            	trace("Process id: " + getPid());
            }
        } catch (Throwable t) {
            log(t);
            t.printStackTrace();
        }
    }

    private void initPackagers() {
        IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(IPackager.EXTENSION_POINT);
        for (int i = 0; i < elements.length; i++) {
            String pattern = elements[i].getAttribute(PackagerProxy.PATTERN);
            Pattern platformPattern = Pattern.compile(pattern);
            patterns.add(platformPattern);
            packagers.add(new PackagerProxy(elements[i]));
        }
    }

    /**
     * Returns the packager for a specific platform.
     * @param platform
     * @return A non-null packager. If no packager is found,
     * a default <code>ErrorPackager</code> is returned
     * @see ErrorPackager
     */
    public IPackager getPackager(String platform) {
        for (int i = 0; i < patterns.size(); i++) {
            Pattern pattern = patterns.get(i);
            if (pattern.matcher(platform).matches()) {
                return packagers.get(i);
            }
        }

        return ErrorPackager.getDefault();
    }

	/**
     * Returns a sorted list of all panic error codes.
     * @return
     */
    public Integer[] getAllPanicErrorCodes() {
        return sortedPanicErrorCodes;
    }

    /**
     * Returns the panic message corresponding to <code>errcode</code>
     * @param errcode
     * @return
     */
    public String getPanicMessage(int errcode) {
        return panicMessages.getProperty(Integer.toString(errcode));
    }


    /**
     * @deprecated Do we really need this? It is never used outside
     * the builder + recalculated every time...
     * @return
     */
    public DependencyManager<IProject> getProjectDependencyManager() {
    	return getProjectDependencyManager(ResourcesPlugin.getWorkspace());
    }

    /**
     * @deprecated Do we really need this? It is never used outside
     * the builder + recalculated every time...
     * @param ws
     * @return
     */
    public DependencyManager<IProject> getProjectDependencyManager(IWorkspace ws) {
    	return projectDependencyManager;
    }
    
    /**
     * Returns the Eclipse OS Process ID.
     * @return
     */
    public static String getPid() {
        return "" + PID.INSTANCE.pid();
    }

    public IProcessUtil getProcessUtil() {
    	return PROCESS.INSTANCE;
    }
    
    /**
     * <p>Outputs a trace message.</p>
     * <p>Please use this pattern:
     * <blockquote><code>
     *     if (CoreMoSyncPlugin.getDefault().isDebugging()) {
     *         trace("A trace message");
     *     }
     * </code></blockquote>
     * </p>
     * @param msg
     */
	public static void trace(Object msg) {
		System.out.println(msg);
	}

    /**
     * <p>Outputs a trace message.</p>
     * <p>The arguments match those of <code>MessageFormat.format</code>.</p>
     * @see {@link CoreMoSyncPlugin#trace(Object)};
     */
	public static void trace(String msg, Object... args) {
		trace(MessageFormat.format(msg, args));
	}

	private void initDeviceFilterFactories() {
		factories = new HashMap<String, IDeviceFilterFactory>();
		// We'll just add some of them explicitly
		factories.put(ConstantFilterFactory.ID, new ConstantFilterFactory());
		factories.put(VendorFilterFactory.ID, new VendorFilterFactory());
		factories.put(FeatureFilterFactory.ID, new FeatureFilterFactory());
		factories.put(ProfileFilterFactory.ID, new ProfileFilterFactory());
		
		IConfigurationElement[] factoryCEs = Platform.getExtensionRegistry().getConfigurationElementsFor(PLUGIN_ID + ".filter.factories");
		for (int i = 0; i < factoryCEs.length; i++) {
			IConfigurationElement factoryCE = factoryCEs[i];
			String id = factoryCE.getAttribute("id");
			DeviceFilterFactoryProxy factory = new DeviceFilterFactoryProxy(factoryCE);
			registerDeviceFilterFactory(id, factory);
		}
	}
	
	private void registerDeviceFilterFactory(String id, IDeviceFilterFactory factory) {
		if (factories.containsKey(id)) {
			throw new IllegalStateException("Id already used");
		}
		factories.put(id, factory);
	}
	
	private void installResourceListener() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.PRE_DELETE | IResourceChangeEvent.PRE_BUILD);
	}
	
	private void deinstallResourceListener() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}
	
	/**
	 * <p>Returns an <code>IDeviceFilterFactory</code>.</p>
	 * <p>Examples of <code>IDeviceFilterFactories</code> are
	 * <code>ConstantFilterFactory</code> and <code>VenderFilterFactory</code>. 
	 * @param factoryId
	 * @return
	 */
	public IDeviceFilterFactory getDeviceFilterFactory(String factoryId) {
		if (factoryId == null) {
			return null;			
		}
		
		// Kind of an IElementFactory, but without the UI deps.
		return factories.get(factoryId);
	}
	
	public IProcessConsole createConsole(String consoleName) {
		if (isHeadless) {
			return new LogProcessConsole(consoleName);
		} else {
			return ideProcessConsoleProvider == null ? new LogProcessConsole(consoleName) : ideProcessConsoleProvider.get(consoleName);
		}
	}
		
	public IEmulatorLauncher getEmulatorLauncher(String launcherId) {
		return launchers.get(launcherId);
	}
	
	public IUpdater getUpdater() {
		if (!updaterInitialized) {
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			IConfigurationElement[] elements = registry
					.getConfigurationElementsFor("com.mobilesorcery.sdk.updater");
			if (elements.length > 0) {
				try {
					updater = (IUpdater) elements[0]
							.createExecutableExtension("implementation");
				} catch (CoreException e) {
					getLog().log(e.getStatus());
				}
			}
			
			updaterInitialized = true;
		}
		
		return updater;
	}
	
	private void disposeUpdater() {
	    if (updater != null) {
	        updater.dispose();
	    }
	}

	private void checkAutoUpdate() {
		if (CoreMoSyncPlugin.isHeadless()) {
			return;
		}
		
		String[] args = Platform.getApplicationArgs();
		if (suppressUpdating(args)) {
			return;
		}
		
		IUpdater updater = getUpdater();
		if (updater != null) {
			updater.update(false);
		}
	}

	private boolean suppressUpdating(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if ("-suppress-updates".equals(args[i])) {
				return true;
			}
		}
		
		return false;
	}

	/**
	 * <p>Returns the (single) emulator process manager</p>
	 * @return
	 */
	public EmulatorProcessManager getEmulatorProcessManager() {
		return emulatorProcessManager;
	}
	
	/**
	 * INTERNAL: Clients should not call this method.
	 */
	public void setIDEProcessConsoleProvider(IProvider<IProcessConsole, String> ideProcessConsoleProvider) {
		// I'm lazy - Instead of extension points...
		this.ideProcessConsoleProvider = ideProcessConsoleProvider;
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (MoSyncTool.MOSYNC_HOME_PREF.equals(event.getProperty()) || MoSyncTool.MO_SYNC_HOME_FROM_ENV_PREF.equals(event.getProperty())) {
			initPanicErrorMessages();
		}
		
	}
	
	
	/**
	 * Tries to derive a mosync project from whatever object is passed
	 * as the <code>receiver</code>; this method will accept <code>List</code>s,
	 * <code>IAdaptable</code>s, <code>IResource</code>s, as well as <code>IStructuredSelection</code>s
	 * and then if the project
	 * associated with these is compatible with a MoSyncProject, return that project.
	 */
	// Should it be here?
	public MoSyncProject extractProject(Object receiver) {
		if (receiver instanceof IStructuredSelection) {
			return extractProject(((IStructuredSelection) receiver).toList());
		}
		
        if (receiver instanceof IAdaptable) {
            receiver = ((IAdaptable) receiver).getAdapter(IResource.class);             
        }
        
        if (receiver instanceof List) {
            if (((List)(receiver)).size() == 0) {
                return null;
            }
            
            return extractProject(((List)receiver).get(0));
        }
        
        if(receiver == null) {
            return null;
        }
        
        if (receiver instanceof IResource) {
            IProject project = ((IResource)receiver).getProject();

            try {                
                return MoSyncNature.isCompatible(project) ? MoSyncProject.create(project) : null;
            } catch (CoreException e) {
                return null;
            }
        }
        
        return null;
	}

	public void resourceChanged(IResourceChangeEvent event) {	    
		if (event.getType() == IResourceChangeEvent.PRE_DELETE) {
		    IResource resource = event.getResource();
	        IProject project = (resource != null && resource.getType() == IResource.PROJECT) ? (IProject) resource : null;

			MoSyncProject mosyncProject = MoSyncProject.create(project);
			if (mosyncProject != null) {
			    // So we do not keep any old references to this project
			    mosyncProject.dispose();
			}
		} else if (event.getType() == IResourceChangeEvent.PRE_BUILD && event.getBuildKind() != IncrementalProjectBuilder.CLEAN_BUILD && event.getBuildKind() != IncrementalProjectBuilder.AUTO_BUILD) {
		    Object source = event.getSource();
		    ArrayList<IResource> mosyncProjects = new ArrayList<IResource>();
		    IProject[] projects = null;
		    if (source instanceof IWorkspace) {
		        projects = ((IWorkspace) source).getRoot().getProjects();
		    } else if (source instanceof IProject) {
		        projects = new IProject[] { (IProject) source };
		    }
		    
	        for (int i = 0; projects != null && i < projects.length; i++) {
	            IProject project = projects[i];
	            try {
                    if (MoSyncNature.isCompatible(project)) {
                        mosyncProjects.add(projects[i]);	                
                    }
                } catch (CoreException e) {
                    CoreMoSyncPlugin.getDefault().log(e);
                }
		    }
		    
	        IJobManager jm = Job.getJobManager();
	        Job currentJob = jm.currentJob();
		    if (!MoSyncBuilder.saveAllEditors(mosyncProjects)) {
		        // If this thread is a build job, then cancel.
		        // TODO: Could this somewhere or some day cease to work!   
		        if (currentJob != null) {
		            currentJob.cancel();
		        }
		    }
		}
	}

    public String[] getBuildConfigurationTypes() {
        return buildConfigurationTypes;
    }
    
    /**
     * <p>Returns a working copy of an <code>IApplicationPermissions</code>
     * with default permissions.</p>
     * @param project
     * @return
     */
    public IApplicationPermissions getDefaultPermissions(MoSyncProject project) {
        return ApplicationPermissions.getDefaultPermissions(project);
    }

    /**
     * Logs a specified method ONCE
     * @param e
     * @param token Used to distinguish the source of log messages
     */
    public void logOnce(Exception e, String token) {
        Integer logCount = logCounts.get(token);
        if (logCount == null) {
            logCount = 0;
        }
        
        if (logCount < 1) {
            log(e);
        }
        
        logCount++;
        logCounts.put(token, logCount);
    }

	
}
