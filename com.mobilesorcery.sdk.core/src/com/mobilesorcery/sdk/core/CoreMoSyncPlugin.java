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
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.crypto.spec.PBEKeySpec;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
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
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.core.build.BuildSequence;
import com.mobilesorcery.sdk.core.build.BundleBuildStep;
import com.mobilesorcery.sdk.core.build.CommandLineBuildStep;
import com.mobilesorcery.sdk.core.build.CompileBuildStep;
import com.mobilesorcery.sdk.core.build.CopyBuildResultBuildStep;
import com.mobilesorcery.sdk.core.build.IBuildStepFactory;
import com.mobilesorcery.sdk.core.build.IBuildStepFactoryExtension;
import com.mobilesorcery.sdk.core.build.LinkBuildStep;
import com.mobilesorcery.sdk.core.build.NativeLibBuildStep;
import com.mobilesorcery.sdk.core.build.PackBuildStep;
import com.mobilesorcery.sdk.core.build.ResourceBuildStep;
import com.mobilesorcery.sdk.core.launch.AutomaticEmulatorLauncher;
import com.mobilesorcery.sdk.core.launch.IEmulatorLauncher;
import com.mobilesorcery.sdk.core.launch.MoReLauncher;
import com.mobilesorcery.sdk.core.memory.LowMemoryManager;
import com.mobilesorcery.sdk.core.security.IApplicationPermissions;
import com.mobilesorcery.sdk.core.stats.Stats;
import com.mobilesorcery.sdk.internal.ErrorPackager;
import com.mobilesorcery.sdk.internal.HeadlessUpdater;
import com.mobilesorcery.sdk.internal.PID;
import com.mobilesorcery.sdk.internal.PROCESS;
import com.mobilesorcery.sdk.internal.PackagerProxy;
import com.mobilesorcery.sdk.internal.PropertyInitializerProxy;
import com.mobilesorcery.sdk.internal.RebuildListener;
import com.mobilesorcery.sdk.internal.ReindexListener;
import com.mobilesorcery.sdk.internal.SecurePasswordProvider;
import com.mobilesorcery.sdk.internal.SecureProperties;
import com.mobilesorcery.sdk.internal.dependencies.DependencyManager;
import com.mobilesorcery.sdk.internal.launch.EmulatorLauncherProxy;
import com.mobilesorcery.sdk.internal.security.ApplicationPermissions;
import com.mobilesorcery.sdk.lib.JNALibInitializer;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.filter.DeviceFilterFactoryProxy;
import com.mobilesorcery.sdk.profiles.filter.IDeviceFilterFactory;
import com.mobilesorcery.sdk.profiles.filter.elementfactories.ConstantFilterFactory;
import com.mobilesorcery.sdk.profiles.filter.elementfactories.DeviceCapabilitiesFilterFactory;
import com.mobilesorcery.sdk.profiles.filter.elementfactories.FeatureFilterFactory;
import com.mobilesorcery.sdk.profiles.filter.elementfactories.ProfileFilterFactory;
import com.mobilesorcery.sdk.profiles.filter.elementfactories.VendorFilterFactory;

/**
 * The activator class controls the plug-in life cycle
 */
public class CoreMoSyncPlugin extends AbstractUIPlugin implements IPropertyChangeListener, IResourceChangeListener {

    // The plug-in ID
    public static final String PLUGIN_ID = "com.mobilesorcery.sdk.core";

	/**
	 * A non-UI console id.
	 */
	public static final String LOG_CONSOLE_NAME = "@@log";

	private static final String WORKSPACE_TOKEN_PREF = PLUGIN_ID + ".w.s.token";

	private static final String PREFERRED_LAUNCER_PREF_PREFIX = PLUGIN_ID + "preferred.launcher.";

	// Days since Oct 15, 1528 until Jan 1, 1970
	private static final long DCE_OFFSET = 141427 * 86400;

    // The shared instance
    private static CoreMoSyncPlugin plugin;

	private static LowMemoryManager lowMemoryManager;

	private static ISavePolicy savePolicy;

    private final ArrayList<Pattern> runtimePatterns = new ArrayList<Pattern>();

    private final ArrayList<Pattern> platformPatterns = new ArrayList<Pattern>();

    private final ArrayList<IPackager> packagers = new ArrayList<IPackager>();

    private Map<String, Map<String, IPropertyInitializer>> propertyInitializers = new HashMap<String, Map<String, IPropertyInitializer>>();

	private final HashMap<String, IEmulatorLauncher> launchers = new HashMap<String, IEmulatorLauncher>();

	private DependencyManager<IProject> projectDependencyManager;

    private Properties panicMessages = new Properties();

    private ReindexListener reindexListener;

    private Integer[] sortedPanicErrorCodes;

	private boolean isHeadless = false;

	private HashMap<String, IDeviceFilterFactory> factories;

	private boolean updaterInitialized;

	private IUpdater updater;

	private IProvider<IProcessConsole, String> ideProcessConsoleProvider;

	private EmulatorProcessManager emulatorProcessManager;

    private String[] buildConfigurationTypes;

    private final HashMap<String, Integer> logCounts = new HashMap<String, Integer>();

	private ISecurePropertyOwner secureProperties;

	private final SecurePasswordProvider passwordProvider = new SecurePasswordProvider();

	private String workspaceToken;

	private SecureRandom secureRnd;

	private HashMap<String, IBuildStepFactoryExtension> buildStepExtensions = null;

	private List<String> buildStepFactoryIds;

	private boolean nativeLibsInited = false;

    /**
     * The constructor
     */
    public CoreMoSyncPlugin() {
    }

    @Override
	public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        aboutBoxHack();
        initReIndexerListener();
        initRebuildListener();
        initPackagers();
        initDeviceFilterFactories();
        initPanicErrorMessages();
        initPropertyInitializers();
        initGlobalDependencyManager();
        initEmulatorProcessManager();
        installResourceListener();
        initBuildConfigurationTypes();
        initLaunchers();
        initSecureProperties();
        initWorkspaceToken();
		getPreferenceStore().addPropertyChangeListener(this);
		initializeOnSeparateThread();
    }

    private void aboutBoxHack() {
    	// The about box makes use of system properties; let's set a few.
    	String mosyncVersion = MoSyncTool.getDefault().getVersionInfo(MoSyncTool.BINARY_VERSION);
    	String buildDate = MoSyncTool.getDefault().getVersionInfo(MoSyncTool.BUILD_DATE);
    	String mainGitHash = MoSyncTool.getDefault().getVersionInfo(MoSyncTool.MOSYNC_GIT_HASH);
    	String eclipseGitHash = MoSyncTool.getDefault().getVersionInfo(MoSyncTool.ECLIPSE_GIT_HASH);
    	System.setProperty("MOSYNC_VERSION", mosyncVersion);
    	System.setProperty("MOSYNC_BUILD_DATE", "Build date: " + buildDate);
    	System.setProperty("MOSYNC_MAIN_GIT_HASH", mainGitHash);
    	System.setProperty("MOSYNC_ECLIPSE_GIT_HASH", eclipseGitHash);
	}

	private void initStats() {
		Stats.getStats().start();
	}

	private void initSecureProperties() {
		secureProperties = new SecureProperties(new PreferenceStorePropertyOwner(getPreferenceStore()), getPasswordProvider(), null);
	}

	private void initLaunchers() {
    	// Default and auto always present
		this.launchers.put(AutomaticEmulatorLauncher.ID, new AutomaticEmulatorLauncher());
    	this.launchers.put(MoReLauncher.ID, new MoReLauncher());
    	IConfigurationElement[] launchers = Platform.getExtensionRegistry().getConfigurationElementsFor(IEmulatorLauncher.EXTENSION_POINT_ID);
    	for (int i = 0; i < launchers.length; i++) {
    		IConfigurationElement launcher = launchers[i];
    		String id = launcher.getAttribute("id");
    		this.launchers.put(id, new EmulatorLauncherProxy(launcher));
    	}
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
			@Override
			public void run() {
				initStats();
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
		if (isHeadless) {
			getDefault().getLog().log(new Status(IStatus.INFO, PLUGIN_ID, "Entering headless mode"));
		}
	}

	private void initGlobalDependencyManager() {
    	// Currently, all workspaces share this guy -- fixme later.
        this.projectDependencyManager = new DependencyManager<IProject>();
	}

	private void initRebuildListener() {
        MoSyncProject.addGlobalPropertyChangeListener(new RebuildListener());
    }

    @Override
	public void stop(BundleContext context) throws Exception {
    	// Must be here, before nulling the plugin
        Stats.getStats().stop();
        plugin = null;
        projectDependencyManager = null;
        disposeUpdater();
        MoSyncProject.removeGlobalPropertyChangeListener(reindexListener);
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

    private synchronized void initNativeLibs() {
    	if (nativeLibsInited) {
    		return;
    	}
    	nativeLibsInited = true;
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
        	try {
	            String runtime = elements[i].getAttribute(PackagerProxy.RUNTIME_PATTERN);
	            String platform = elements[i].getAttribute(PackagerProxy.PLATFORM_PATTERN);
	            Pattern runtimePattern = runtime == null ? null : Pattern.compile(runtime);
	            Pattern platformPattern = platform == null ? null : Pattern.compile(platform);
	            runtimePatterns.add(runtimePattern);
	            platformPatterns.add(platformPattern);
	            packagers.add(new PackagerProxy(elements[i]));
        	} catch (Exception e) {
        		CoreMoSyncPlugin.getDefault().log(e);
        	}
        }
    }

    /**
     * Returns the packager for a specific project/platform.
     * @param project The profile type
     * @param profile The profile to package for
     * @return A non-null packager. If no packager is found,
     * a default <code>ErrorPackager</code> is returned
     * @see ErrorPackager
     */
    public IPackager getPackager(int profileType, IProfile profile) {
    	String runtime = profile.getRuntime();
    	String platform = profile.getVendor().getName();
    	IPackager packager = null;
    	if (profileType == MoSyncTool.DEFAULT_PROFILE_TYPE) {
    		packager = matchPackager(platformPatterns, platform);
    	}
    	if (packager == null) {
    		packager = matchPackager(runtimePatterns, runtime);
    	}
    	if (packager == null) {
    		packager = ErrorPackager.getDefault();
    	}
        return packager;
    }

    private IPackager matchPackager(List<Pattern> patterns, String matchingCriteria) {
        for (int i = 0; i < patterns.size(); i++) {
            Pattern pattern = patterns.get(i);
            if (pattern != null && pattern.matcher(matchingCriteria).matches()) {
                return packagers.get(i);
            }
        }
        return null;
    }

    /**
     * Returns an (unmodifiable) list of all available packagers
     * @return
     */
    public List<IPackager> getPackagers() {
    	return Collections.unmodifiableList(packagers);
    }

    /**
     * Returns a packager with a specific id.
     * @param id
     * @return
     */
    public IPackager getPackagerById(String id) {
    	for (IPackager packager : packagers) {
    		if (Util.equals(packager.getId(), id)) {
    			return packager;
    		}
    	}
    	return null;
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
    @Deprecated
	public DependencyManager<IProject> getProjectDependencyManager() {
    	return getProjectDependencyManager(ResourcesPlugin.getWorkspace());
    }

    /**
     * @deprecated Do we really need this? It is never used outside
     * the builder + recalculated every time...
     * @param ws
     * @return
     */
    @Deprecated
	public DependencyManager<IProject> getProjectDependencyManager(IWorkspace ws) {
    	return projectDependencyManager;
    }

    /**
     * Returns the Eclipse OS Process ID.
     * @return
     */
    public String getPid() {
    	initNativeLibs();
        return "" + PID.INSTANCE.pid();
    }

    public IProcessUtil getProcessUtil() {
    	initNativeLibs();
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
     * <p>Long messages will be truncated.</p>
     * @param msg
     */
	public static void trace(Object msg) {
		System.out.println(Util.truncate("" + msg, null, 1024));
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
		factories.put(DeviceCapabilitiesFilterFactory.ID, new DeviceCapabilitiesFilterFactory());

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
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.PRE_DELETE | IResourceChangeEvent.PRE_BUILD | IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.POST_CHANGE);
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

	/**
	 * Creates an {@link IProcessConsole} to be used for output of command line tools,
	 * build plugins, etc.
	 * @param consoleName
	 * @return An {@link IProcessConsole}. If running in headless mode or if the
	 * {@code consoleName} is set to {@link #LOG_CONSOLE_NAME}, a non-UI console is
	 * returned.
	 */
	public IProcessConsole createConsole(String consoleName) {
		if (isHeadless || LOG_CONSOLE_NAME.equals(consoleName)) {
			return new LogProcessConsole(consoleName);
		} else {
			return ideProcessConsoleProvider == null ? new LogProcessConsole(consoleName) : ideProcessConsoleProvider.get(consoleName);
		}
	}

	/**
	 * For a given launcher id, return the corresponding {@link IEmulatorLauncher}.
	 * @param launcherId
	 * @return
	 */
	public IEmulatorLauncher getEmulatorLauncher(String launcherId) {
		return launchers.get(launcherId);
	}

	/**
	 * Returns the preferred launcher for a given packager.
	 * @param packager
	 * @return {@code null} if no preferred launcher
	 */
	public IEmulatorLauncher getPreferredLauncher(String packager) {
		IPreferenceStore store = getPreferenceStore();
		String launcherId = store.getString(PREFERRED_LAUNCER_PREF_PREFIX + packager);
		return getEmulatorLauncher(launcherId);
	}

	/**
	 * Sets the preferred launcher for a given packager.
	 * @param packager
	 * @param launcherId {@code null} if no preferred launcher should be set
	 */
	public void setPreferredLauncher(String packager, String launcherId) {
		IPreferenceStore store = getPreferenceStore();
		String pref = PREFERRED_LAUNCER_PREF_PREFIX + packager;
		if (launcherId == null) {
			store.setToDefault(pref);
		} else {
			store.setValue(pref, launcherId);
		}
	}


	public Set<String> getEmulatorLauncherIds() {
		return Collections.unmodifiableSet(launchers.keySet());
	}

	public IBuildStepFactory createBuildStepFactory(String id) {
		// The default ones.
		if (CompileBuildStep.ID.equals(id)) {
			return new CompileBuildStep.Factory();
		} else if (ResourceBuildStep.ID.equals(id)) {
			return new ResourceBuildStep.Factory();
		} else if (LinkBuildStep.ID.equals(id)) {
			return new LinkBuildStep.Factory();
		} else if (PackBuildStep.ID.equals(id)) {
			return new PackBuildStep.Factory();
		} else if (CommandLineBuildStep.ID.equals(id)) {
			return new CommandLineBuildStep.Factory();
		} else if (BundleBuildStep.ID.equals(id)) {
			return new BundleBuildStep.Factory();
		} else if (CopyBuildResultBuildStep.ID.equals(id)) {
			return new CopyBuildResultBuildStep.Factory();
		} else if (NativeLibBuildStep.ID.equals(id)) {
			return new NativeLibBuildStep.Factory();
		}

		IBuildStepFactoryExtension extension = getBuildStepFactoryExtension(id);
		if (extension != null) {
			return extension.createFactory();
		}
		return null;
	}
	
	public IBuildStepFactoryExtension getBuildStepFactoryExtension(String id) {
		if (buildStepExtensions == null) {
			buildStepExtensions = new HashMap<String, IBuildStepFactoryExtension>();
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			IConfigurationElement[] elements = registry
					.getConfigurationElementsFor(IBuildStepFactoryExtension.EXTENSION_ID);
			for (IConfigurationElement element : elements) {
				try {
					String extId = element.getAttribute("id");
					IBuildStepFactoryExtension ext = (IBuildStepFactoryExtension) element.createExecutableExtension("implementation");
					buildStepExtensions.put(extId,  ext);
				} catch (CoreException e) {
					CoreMoSyncPlugin.getDefault().log(e);
				}
			}
		}
		return buildStepExtensions.get(id);
	}
	
	/**
	 * Returns a mutable list of all build step factories.
	 * @return
	 */
	public List<String> getBuildStepFactories() {
		if (buildStepFactoryIds == null) {
			ArrayList<String> result = new ArrayList<String>();
			result.add(CompileBuildStep.ID);
			result.add(ResourceBuildStep.ID);
			result.add(LinkBuildStep.ID);
			result.add(PackBuildStep.ID);
			result.add(CommandLineBuildStep.ID);
			result.add(BundleBuildStep.ID);
			result.add(CopyBuildResultBuildStep.ID);
			getBuildStepFactoryExtension(""); // Just to init.
			result.addAll(buildStepExtensions.keySet());
			buildStepFactoryIds = Collections.unmodifiableList(result);
		}
		return buildStepFactoryIds;
	}

	public IUpdater getUpdater() {
		if (isHeadless) {
			if (isDebugging()) {
				trace("Headless build: update checks suppressed");
			}
			return HeadlessUpdater.getInstance();
		} else if (!updaterInitialized) {
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

	public void checkAutoUpdate() {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				internalCheckAutoUpdate();
			}
		});
		t.setName("Auto update");
		t.start();
	}

	private synchronized void internalCheckAutoUpdate() {
		if (isHeadless) {
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

	@Override
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

        if (receiver instanceof Collection) {
            if (((Collection)(receiver)).size() == 0) {
                return null;
            }

            return extractProject(((Collection)receiver).iterator().next());
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

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		int eventType = event.getType();
		if (eventType == IResourceChangeEvent.PRE_DELETE || eventType == IResourceChangeEvent.PRE_CLOSE) {
		    IResource resource = event.getResource();
	        IProject project = (resource != null && resource.getType() == IResource.PROJECT) ? (IProject) resource : null;

			MoSyncProject mosyncProject = MoSyncProject.create(project);
			if (mosyncProject != null) {
			    // So we do not keep any old references to this project
			    mosyncProject.dispose();
			}
		} else if (eventType == IResourceChangeEvent.PRE_BUILD && event.getBuildKind() != IncrementalProjectBuilder.CLEAN_BUILD && event.getBuildKind() != IncrementalProjectBuilder.AUTO_BUILD) {
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
		} else {
			Collection<MoSyncProject> projects = extractProjectsToReinit(event);
			for (MoSyncProject project : projects) {
				project.reinit(true);
			}
		}
	}

	public Collection<MoSyncProject> extractProjectsToReinit(IResourceChangeEvent event) {
		final HashSet<MoSyncProject> result = new HashSet<MoSyncProject>();
		boolean isContentChange = event.getDelta() != null && (event.getDelta().getFlags() & IResourceDelta.CONTENT) != 0;
		boolean isFileResource = event.getResource() != null && event.getResource().getType() == IResource.FILE;
		if (event.getType() == IResourceChangeEvent.POST_CHANGE && isContentChange && isFileResource) {
			try {
				event.getDelta().accept(new IResourceDeltaVisitor() {
					@Override
					public boolean visit(IResourceDelta delta) throws CoreException {
						IResource resource = delta.getResource();
						if (resource != null) {
							String name = resource.getName();
							if (MoSyncProject.MOSYNC_PROJECT_META_DATA_FILENAME.equals(name) ||
								MoSyncProject.MOSYNC_PROJECT_META_DATA_FILENAME.equals(name)) {
								MoSyncProject mosyncProject = MoSyncProject.create(resource.getProject());
								if (mosyncProject != null) {
									result.add(mosyncProject);
									// And for good measure...
									BuildSequence.clearCache(mosyncProject);
								}
							}
						}
						return true;
					}
				});
			} catch (CoreException e) {
				CoreMoSyncPlugin.getDefault().log(e);
			}
		}
		return result;
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

	public ISecurePropertyOwner getSecureProperties() {
		return secureProperties;
	}

	public IProvider<PBEKeySpec, String> getPasswordProvider() {
		return passwordProvider;
	}

	public boolean usesEclipseSecureStorage() {
		return passwordProvider.usesEclipseSecureStorage();
	}

	public void doUseEclipseSecureStorage(boolean useEclipseSecureStorage) throws CoreException {
		passwordProvider.doUseEclipseSecureStorage(useEclipseSecureStorage);
	}

	/**
	 * Returns a 'workspace' token, that has a very large probability
	 * of being unique per workspace. (It is randomly generated).
	 * It is designed be used in filenames for workspace specific file lookup.
	 * @return
	 */
	public String getWorkspaceToken() {
		return workspaceToken;
	}

	private void initWorkspaceToken() {
		if (getPreferenceStore().isDefault(WORKSPACE_TOKEN_PREF)) {
			byte[] random = new byte[6];
			new Random(System.currentTimeMillis()).nextBytes(random);
			String workspaceToken = Util.toBase16(random);
			getPreferenceStore().setValue(WORKSPACE_TOKEN_PREF, workspaceToken);
		}
		this.workspaceToken = getPreferenceStore().getString(WORKSPACE_TOKEN_PREF);
	}

	/**
	 * Generates a time-based 128-bit UUID.
	 * @return
	 */
	public UUID generateUUID() {
		if (secureRnd == null) {
			secureRnd = new SecureRandom();
		}
		// Generate a 48-bit random node id; set the multicast bit to 1
		long nodeId = (secureRnd.nextLong() & 0xffffffffffffL) | 0x010000000000L;
		long clockSeq = secureRnd.nextInt(0x4fff);
		long clockSeq_lo = clockSeq & 0xff;
		long clockSeq_hi_and_res = ((clockSeq >> 16) & 0x4f) | 0x80;
		long lsb = (clockSeq_hi_and_res << 56) | (clockSeq_lo << 48) | nodeId;

		long utcTimestamp = System.currentTimeMillis();
		long timestamp = DCE_OFFSET + 10000 * utcTimestamp;
		long timestamp_lo = timestamp & 0xffffffff;
		long timestamp_mid = (timestamp >> 32) & 0xffff;
		long timestamp_hi_and_version = ((timestamp >> 48) & 0x0fff) | 0x1000;
		long msb = (timestamp_lo << 32) | (timestamp_mid << 16) | timestamp_hi_and_version;
		return new UUID(msb, lsb);
	}

	/**
	 * Returns the 'low memory manager' for notifications on low memory
	 * @return
	 */
	public static synchronized LowMemoryManager getLowMemoryManager() {
		if (lowMemoryManager == null) {
			lowMemoryManager = new LowMemoryManager(0.85);
		}
		return lowMemoryManager;
	}

	public static ISavePolicy getSavePolicy() {
		if (savePolicy == null) {
			IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(ISavePolicy.EXTENSION_POINT);
	        try {
	        	savePolicy = ISavePolicy.NULL;
	        	if (!isHeadless() && elements.length == 1) {
	        		savePolicy = (ISavePolicy) elements[0].createExecutableExtension("implementation");
	        	}
	        } catch (Exception e) {
	        	// Ignore
	        }
		}
		return savePolicy;
	}

}
