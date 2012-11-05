package com.mobilesorcery.sdk.html5;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.jface.util.Policy;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.LibrarySuperType;
import org.eclipse.wst.jsdt.internal.core.JavaProject;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.core.BuildVariant;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IProcessConsole;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PrivilegedAccess;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.build.BuildSequence;
import com.mobilesorcery.sdk.core.build.IBuildStepFactory;
import com.mobilesorcery.sdk.core.build.ResourceBuildStep;
import com.mobilesorcery.sdk.html5.debug.JSODDLaunchConfigurationDelegate;
import com.mobilesorcery.sdk.html5.debug.JSODDSupport;
import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;
import com.mobilesorcery.sdk.html5.live.ILiveServerListener;
import com.mobilesorcery.sdk.html5.live.JSODDServer;
import com.mobilesorcery.sdk.html5.live.ReloadManager;
import com.mobilesorcery.sdk.html5.ui.DebuggingEnableTester;
import com.mobilesorcery.sdk.html5.ui.JSODDConnectDialog;
import com.mobilesorcery.sdk.html5.ui.JSODDTimeoutDialog;
import com.mobilesorcery.sdk.internal.launch.EmulatorLaunchConfigurationDelegate;
import com.mobilesorcery.sdk.profiles.filter.DeviceCapabilitiesFilter;
import com.mobilesorcery.sdk.ui.IWorkbenchStartupListener;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.UIUtils;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhoneTransportListener;
import com.mobilesorcery.sdk.ui.targetphone.TargetPhonePlugin;
import com.mobilesorcery.sdk.ui.targetphone.TargetPhoneTransportEvent;

/**
 * The activator class controls the plug-in life cycle
 */
public class Html5Plugin extends AbstractUIPlugin implements IStartup,
		ITargetPhoneTransportListener, ILaunchesListener2 {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.mobilesorcery.sdk.html5"; //$NON-NLS-1$

	public static final String JS_PROJECT_SUPPORT_PROP = PLUGIN_ID + ".support";

	public static final String HTML5_TEMPLATE_TYPE = "html5";

	public static final String ODD_SUPPORT_PROP = PLUGIN_ID + ".odd";

	public static final String ANONYMOUS_FUNCTION = "<anonymous>";

	static final String RELOAD_STRATEGY_PREF = "reload.strategy";

	static final String SOURCE_CHANGE_STRATEGY_PREF = "source.change.strategy";

	static final String SHOULD_FETCH_REMOTELY_PREF = "fetch.remotely";

	static final String ODD_SUPPORT_PREF = "odd.support";

	static final String USE_DEFAULT_SERVER_URL_PREF = "use.default.server";

	static final String SERVER_URL_PREF = "server";

	public static final String TIMEOUT_PREF = "timeout";

	public static final int DEFAULT_TIMEOUT = 5;

	public static final int MINIMUM_TIMEOUT = 2;

	public static final int DO_NOTHING = 0;

	public static final int RELOAD = 1;

	public static final int HOT_CODE_REPLACE = 2;

	public static final String TERMINATE_TOKEN_LAUNCH_ATTR = PLUGIN_ID + "t.t";

	public static final String SUPPRESS_TIMEOUT_LAUNCH_ATTR = PLUGIN_ID + "s.t";

	// The shared instance
	private static Html5Plugin plugin;

	private ReloadManager reloadManager;

	private JSODDServer server;

	private final HashMap<IProject, JSODDSupport> jsOddSupport = new HashMap<IProject, JSODDSupport>();

	// private HashMap<Object, AtomicInteger> timeoutSuppressions = new
	// HashMap<Object, AtomicInteger>();

	/**
	 * The constructor
	 */
	public Html5Plugin() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		// We do not yet have the eclipse fix #54993
		MosyncUIPlugin.getDefault().awaitWorkbenchStartup(
				new IWorkbenchStartupListener() {
					@Override
					public void started(IWorkbench workbench) {
						DebugPlugin.getDefault().getBreakpointManager()
								.getBreakpoints();
					}
				});
		plugin = this;

		// Since we are an IStartup, this will work.
		TargetPhonePlugin.getDefault().addTargetPhoneTransportListener(this);
		DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);

		initReloadManager();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
		TargetPhonePlugin.getDefault().removeTargetPhoneTransportListener(this);
		DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);
		disposeReloadManager();
	}

	public JSODDServer getReloadServer() {
		if (server == null) {
			server = new JSODDServer();
			// TODO:MOVE UI STUFF
			server.addListener(new ILiveServerListener() {
				@Override
				public void timeout(final ReloadVirtualMachine vm) {
					IProcessConsole console = CoreMoSyncPlugin.getDefault().createConsole(MoSyncBuilder.CONSOLE_ID);
					console.addMessage(IProcessConsole.ERR, MessageFormat.format("*** A timeout occurred. The device being debugged (at {0}) seems to have been disconnected. ***", vm.getRemoteAddr()));
					ILaunch launch = vm.getJavaScriptDebugTarget().getLaunch();
					String terminateToken = launch == null ? null : launch
							.getAttribute(TERMINATE_TOKEN_LAUNCH_ATTR);
					if (!Boolean.parseBoolean(launch
							.getAttribute(SUPPRESS_TIMEOUT_LAUNCH_ATTR))) {
						Display d = PlatformUI.getWorkbench().getDisplay();
						UIUtils.onUiThread(d, new Runnable() {
							@Override
							public void run() {
								Shell shell = PlatformUI.getWorkbench()
										.getActiveWorkbenchWindow().getShell();
								JSODDTimeoutDialog.openIfNecessary(shell, vm);
							}
						}, false);
					} else {
						if (CoreMoSyncPlugin.getDefault().isDebugging()) {
							CoreMoSyncPlugin.trace(
									"Suppressed timeout dialog for {0}.",
									terminateToken);
						}
					}
				}

				@Override
				public void inited(ReloadVirtualMachine vm, boolean reset) {

				}
			});
		}
		return server;
	}

	private void initReloadManager() {
		this.reloadManager = new ReloadManager();
	}

	private void disposeReloadManager() {
		if (reloadManager != null) {
			reloadManager.dispose();
		}
		this.reloadManager = null;
	}

	/**
	 * @deprecated Only to get hold of file id , refactor!!!
	 * @return
	 */
	@Deprecated
	public ReloadManager getReloadManager() {
		return reloadManager;
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Html5Plugin getDefault() {
		return plugin;
	}

	/**
	 * Adds HTML5 support to a {@link MoSyncProject}.
	 * 
	 * @param configureForODD
	 * 
	 * @throws CoreException
	 */
	public void addHTML5Support(MoSyncProject project, boolean configureForODD)
			throws CoreException {
		try {
			BuildSequence sequence = new BuildSequence(project);
			List<IBuildStepFactory> factories = sequence
					.getBuildStepFactories();
			ArrayList<IBuildStepFactory> newFactories = new ArrayList<IBuildStepFactory>();
			for (IBuildStepFactory factory : factories) {
				if (ResourceBuildStep.ID.equals(factory.getId())) {
					newFactories.add(createHTML5PackagerBuildStep());
				}
				newFactories.add(factory);
			}
			sequence.apply(newFactories);
			PrivilegedAccess.getInstance().grantAccess(project, true);
			PropertyUtil.setBoolean(project, JS_PROJECT_SUPPORT_PROP, true);

			configureForJSDT(project);
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID,
					"Could not create JavaScript/HTML5 project", e));
		}
	}

	private void configureForJSDT(MoSyncProject mosyncProject)
			throws CoreException {
		IProject project = mosyncProject.getWrappedProject();
		addJavaScriptNature(project);
		IJavaScriptProject jsProject = JavaScriptCore.create(project);
		if (jsProject instanceof JavaProject) {
			JavaProject jsProject1 = (JavaProject) jsProject;
			jsProject1.setCommonSuperType(new LibrarySuperType(new Path(
					"org.eclipse.wst.jsdt.launching.baseBrowserLibrary"),
					jsProject1, "Window"));
		} else {
			CoreMoSyncPlugin
					.getDefault()
					.logOnce(
							new IllegalStateException("Invalid JSDT version!!"),
							"JSDT");
		}

		// Add HTML5 capability filter!
		DeviceCapabilitiesFilter oldFilter = DeviceCapabilitiesFilter
				.extractFilterFromProject(mosyncProject);
		HashSet<String> newCapabilities = new HashSet<String>(
				oldFilter.getRequiredCapabilities());
		newCapabilities.add("HTML5");
		DeviceCapabilitiesFilter newFilter = DeviceCapabilitiesFilter.create(
				newCapabilities.toArray(new String[0]), new String[0]);
		DeviceCapabilitiesFilter.setFilter(mosyncProject, newFilter);
	}

	private void addJavaScriptNature(IProject project) throws CoreException {
		if (project.hasNature(JavaScriptCore.NATURE_ID)) {
			return;
		}

		IProjectDescription description = project.getDescription();
		String[] natures = description.getNatureIds();
		String[] newNatures = new String[natures.length + 1];
		System.arraycopy(natures, 0, newNatures, 0, natures.length);
		newNatures[newNatures.length - 1] = JavaScriptCore.NATURE_ID;
		description.setNatureIds(newNatures);
		project.setDescription(description, new NullProgressMonitor());
	}

	public boolean hasHTML5Support(MoSyncProject project) {
		if (project == null) {
			return false;
		}
		DeviceCapabilitiesFilter filter = DeviceCapabilitiesFilter
				.extractFilterFromProject(project);
		boolean hasHTML5Capability = filter != null
				&& filter.getRequiredCapabilities().contains("HTML5");
		boolean hasSupport = hasHTML5Capability
				&& PropertyUtil.getBoolean(project, JS_PROJECT_SUPPORT_PROP);
		return hasSupport;
	}

	public boolean hasHTML5PackagerBuildStep(MoSyncProject project) {
		BuildSequence seq = BuildSequence.getCached(project);
		return !seq.getBuildStepFactories(
				HTML5DebugSupportBuildStep.Factory.class).isEmpty();
	}

	private IBuildStepFactory createHTML5PackagerBuildStep() {
		/*
		 * BundleBuildStep.Factory factory = new BundleBuildStep.Factory();
		 * factory.setFailOnError(true);
		 * factory.setName("HTML5/JavaScript bundling");
		 * factory.setInFile("%current-project%/LocalFiles");
		 * factory.setOutFile("%current-project%/Resources/LocalFiles.bin");
		 * return factory;
		 */
		// BAH -- We do NOT always want to copy LocalFiles to package etc
		HTML5DebugSupportBuildStep.Factory factory = new HTML5DebugSupportBuildStep.Factory();
		return factory;
	}

	public synchronized JSODDSupport getJSODDSupport(IProject project) {
		if (!DebuggingEnableTester.hasDebugSupport(MoSyncProject
				.create(project))) {
			return null;
		}
		JSODDSupport result = jsOddSupport.get(project);
		if (result == null) {
			result = new JSODDSupport(project);
			jsOddSupport.put(project, result);
		}
		return result;
	}

	public boolean hasJSODDSupport(IProject project) {
		return getJSODDSupport(project) != null;
	}

	@Override
	public void earlyStartup() {
		// Just to activate the bundle.
	}

	public Collection<IProject> getProjectsWithJSODDSupport() {
		return Collections.unmodifiableCollection(jsOddSupport.keySet());
	}

	/**
	 * Returns the path where HTML5 content is stored. The path is project
	 * relative.
	 * 
	 * @param wrappedProject
	 * @return
	 */
	public static IPath getHTML5Folder(IProject project) {
		// I guess this might always be constant...
		return new Path("LocalFiles");
	}

	/**
	 * Returns the 'local path' of a file, i e where it is located related to
	 * its LocalFiles directory.
	 * 
	 * @param file
	 * @return
	 */
	public IPath getLocalPath(IFile file) {
		IPath root = file.getProject()
				.getFolder(Html5Plugin.getHTML5Folder(file.getProject()))
				.getFullPath();
		if (root.isPrefixOf(file.getFullPath())) {
			return file.getFullPath().removeFirstSegments(root.segmentCount());
		}
		return null;
	}

	public IResource getLocalFile(IProject project, IPath path) {
		return project.getFolder(getHTML5Folder(project)).findMember(path);
	}

	public int getTimeout() {
		int result = getPreferenceStore().getInt(TIMEOUT_PREF);
		if (result < MINIMUM_TIMEOUT) {
			result = MINIMUM_TIMEOUT; // Minimum; regardless of store value
		}
		return result;
	}

	public void setTimeout(int timeout) {
		getPreferenceStore().setValue(TIMEOUT_PREF, timeout);
	}

	public int getReloadStrategy() {
		// TODO. Default = 0 = UNDEFINED
		return getPreferenceStore().getInt(RELOAD_STRATEGY_PREF);
	}

	public void setReloadStrategy(int reloadStrategy) {
		getPreferenceStore().setDefault(RELOAD_STRATEGY_PREF, reloadStrategy);
	}

	public void setSourceChangeStrategy(int sourceChangeStrategy) {
		getPreferenceStore().setValue(SOURCE_CHANGE_STRATEGY_PREF,
				sourceChangeStrategy);
	}

	public int getSourceChangeStrategy() {
		return getPreferenceStore().getInt(SOURCE_CHANGE_STRATEGY_PREF);
	}

	public boolean shouldFetchRemotely() {
		boolean shouldFetchRemotely = getPreferenceStore().getBoolean(
				SHOULD_FETCH_REMOTELY_PREF);
		return shouldFetchRemotely || getSourceChangeStrategy() != DO_NOTHING;
	}

	public void setShouldFetchRemotely(boolean shouldFetchRemotely) {
		getPreferenceStore().setValue(SHOULD_FETCH_REMOTELY_PREF,
				shouldFetchRemotely);
	}

	public boolean isJSODDEnabled(MoSyncProject project) {
		return PropertyUtil.getBoolean(project, ODD_SUPPORT_PREF);
	}

	public void setJSODDEnabled(MoSyncProject project, boolean enabled) {
		PropertyUtil.setBoolean(project, ODD_SUPPORT_PREF, enabled);
	}

	@Override
	public void launchesRemoved(ILaunch[] launches) {
		// We don't care.
	}

	@Override
	public void launchesAdded(ILaunch[] launches) {
		// We do care :)
		for (ILaunch launch : launches) {
			ILaunchConfiguration cfg = launch.getLaunchConfiguration();
			if (cfg != null) {
				try {
					String cfgId = cfg.getType().getIdentifier();
					if (EmulatorLaunchConfigurationDelegate.ID.equals(cfgId)) {
						IProject project = EmulatorLaunchConfigurationDelegate
								.getProject(cfg);
						IBuildVariant variant = EmulatorLaunchConfigurationDelegate
								.getVariant(cfg, launch.getLaunchMode());
						launchJSODD(MoSyncProject.create(project), variant,
								false, BuildVariant.toString(variant));
					}
				} catch (Exception e) {
					// Who cares?
					CoreMoSyncPlugin.getDefault().log(e);
				}
			}
		}
	}

	@Override
	public void launchesChanged(ILaunch[] launches) {
		// We don't care
	}

	@Override
	public void launchesTerminated(ILaunch[] launches) {

	}

	@Override
	public void handleEvent(TargetPhoneTransportEvent event) {
		// Launch the debug server if sending package in debug mode
		if (TargetPhoneTransportEvent.isType(
				TargetPhoneTransportEvent.ABOUT_TO_LAUNCH, event)) {
			MoSyncProject project = event.project;
			IBuildVariant variant = event.variant;
			launchJSODD(project, variant, true, event.phone.getName());
		}
	}

	private void launchJSODD(final MoSyncProject project,
			final IBuildVariant variant, final boolean onDevice,
			final String terminateToken) {
		IPropertyOwner properties = MoSyncBuilder.getPropertyOwner(project,
				variant.getConfigurationId());
		if (DebuggingEnableTester.hasDebugSupport(project)
				&& PropertyUtil.getBoolean(properties,
						MoSyncBuilder.USE_DEBUG_RUNTIME_LIBS) && isJSODDEnabled(project)) {

			new Thread(new Runnable() {
				public void run() {
					try {
						boolean wasLaunched = JSODDLaunchConfigurationDelegate
								.launchDefault(terminateToken);
						int result = JSODDConnectDialog.show(project, variant,
								onDevice, null);
						if (result == JSODDConnectDialog.CANCEL) {
							JSODDLaunchConfigurationDelegate
									.killLaunch(terminateToken);
						}
					} catch (CoreException e) {
						Policy.getStatusHandler()
								.show(e.getStatus(),
										"Could not launch JavaScript On-Device Debug Server");
					}
				}
			}).start();
		}
	}

	/*
	 * private int incTimeoutSuppression(Object terminateToken, int increment) {
	 * // We don't scare users with timeouts if we just // started another
	 * session for potentially the same // device / app (since a timeout is
	 * expected // and not an error condition. AtomicInteger suppressionCount =
	 * timeoutSuppressions.get(terminateToken); if (suppressionCount == null) {
	 * suppressionCount = new AtomicInteger();
	 * timeoutSuppressions.put(terminateToken, suppressionCount); } int result =
	 * suppressionCount.addAndGet(increment); if (result == 0) {
	 * timeoutSuppressions.remove(terminateToken); } return result; }
	 */

	public URL getServerURL() throws IOException {
		if (useDefaultServerURL()) {
			return getDefaultServerURL();
		} else {
			return new URL(getPreferenceStore().getString(SERVER_URL_PREF));
		}
	}

	public boolean useDefaultServerURL() {
		return getPreferenceStore().getBoolean(USE_DEFAULT_SERVER_URL_PREF);
	}

	public void setServerURL(String addr, boolean useDefault)
			throws IOException {
		// Just to get an exception.
		URL url = new URL(addr);
		getPreferenceStore().setValue(USE_DEFAULT_SERVER_URL_PREF, useDefault);
		getPreferenceStore().setValue(SERVER_URL_PREF, addr);
	}

	public URL getDefaultServerURL() throws IOException {
		InetAddress localHost = InetAddress.getLocalHost();
		String host = localHost.getHostAddress();
		return new URL("http", host, 8511, "");
	}

	public boolean isFeatureSupported(String feature) {
		// Ok, one more tricky thing left: binding of function defined
		// within function - then we can enable this.
		return !JSODDSupport.EDIT_AND_CONTINUE.equals(feature);
	}

}
