package com.mobilesorcery.sdk.html5;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.LibrarySuperType;
import org.eclipse.wst.jsdt.internal.core.JavaProject;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PrivilegedAccess;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.build.BuildSequence;
import com.mobilesorcery.sdk.core.build.IBuildStepFactory;
import com.mobilesorcery.sdk.core.build.ResourceBuildStep;
import com.mobilesorcery.sdk.html5.debug.JSODDSupport;
import com.mobilesorcery.sdk.html5.live.LiveServer;
import com.mobilesorcery.sdk.html5.live.ReloadManager;
import com.mobilesorcery.sdk.profiles.filter.DeviceCapabilitiesFilter;
import com.mobilesorcery.sdk.ui.IWorkbenchStartupListener;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;

/**
 * The activator class controls the plug-in life cycle
 */
public class Html5Plugin extends AbstractUIPlugin implements IStartup {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.mobilesorcery.sdk.html5"; //$NON-NLS-1$

	private static final String JS_PROJECT_SUPPORT_PROP = PLUGIN_ID
			+ ".support";

	public static final String HTML5_TEMPLATE_TYPE = "html5";

	public static final String ODD_SUPPORT_PROP = PLUGIN_ID + ".odd";

	// The shared instance
	private static Html5Plugin plugin;

	private ReloadManager reloadManager;

	private LiveServer server;

	private final HashMap<IProject, JSODDSupport> jsOddSupport = new HashMap<IProject, JSODDSupport>();

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
		MosyncUIPlugin.getDefault().awaitWorkbenchStartup(new IWorkbenchStartupListener() {
			@Override
			public void started(IWorkbench workbench) {
				DebugPlugin.getDefault().getBreakpointManager().getBreakpoints();
			}
		});
		plugin = this;
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
		disposeReloadManager();
	}

	public LiveServer getReloadServer() {
		if (server == null) {
			server = new LiveServer();
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
	 * @param configureForODD
	 *
	 * @throws CoreException
	 */
	public void addHTML5Support(MoSyncProject project, boolean configureForODD) throws CoreException {
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
		DeviceCapabilitiesFilter oldFilter = DeviceCapabilitiesFilter.extractFilterFromProject(mosyncProject);
		HashSet<String> newCapabilities = new HashSet<String>(oldFilter.getRequiredCapabilities());
		newCapabilities.add("HTML5");
		DeviceCapabilitiesFilter newFilter = DeviceCapabilitiesFilter.create(newCapabilities.toArray(new String[0]), new String[0]);
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
		return PropertyUtil.getBoolean(project, JS_PROJECT_SUPPORT_PROP);
	}

	private IBuildStepFactory createHTML5PackagerBuildStep() {
		/*BundleBuildStep.Factory factory = new BundleBuildStep.Factory();
		factory.setFailOnError(true);
		factory.setName("HTML5/JavaScript bundling");
		factory.setInFile("%current-project%/LocalFiles");
		factory.setOutFile("%current-project%/Resources/LocalFiles.bin");
		return factory;*/
		// BAH -- We do NOT always want to copy LocalFiles to package etc
		HTML5DebugSupportBuildStep.Factory factory = new HTML5DebugSupportBuildStep.Factory();
		return factory;
	}

	public synchronized JSODDSupport getJSODDSupport(IProject project) {
		if (MoSyncProject.create(project) == null) {
			return null;
		}
		JSODDSupport result = jsOddSupport.get(project);
		if (result == null) {
			result = new JSODDSupport(project);
			jsOddSupport.put(project, result);
		}
		return result;
	}

	@Override
	public void earlyStartup() {
		// Just to activate the bundle.
	}

	public Collection<IProject> getProjectsWithJSODDSupport() {
		return Collections.unmodifiableCollection(jsOddSupport.keySet());
	}
}
