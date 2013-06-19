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
package com.mobilesorcery.sdk.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.Util;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.activities.ICategory;
import org.eclipse.ui.activities.ICategoryActivityBinding;
import org.eclipse.ui.activities.IWorkbenchActivitySupport;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.ide.undo.AbstractWorkspaceOperation;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.core.Cache;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IProcessConsole;
import com.mobilesorcery.sdk.core.IProvider;
import com.mobilesorcery.sdk.core.IsExperimentalTester;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncNature;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.NameSpacePropertyOwner;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.build.CommandLineBuildStep;
import com.mobilesorcery.sdk.core.build.CopyBuildResultBuildStep;
import com.mobilesorcery.sdk.core.build.IBuildStepFactory;
import com.mobilesorcery.sdk.core.build.IBuildStepFactoryExtension;
import com.mobilesorcery.sdk.core.launch.AutomaticEmulatorLauncher;
import com.mobilesorcery.sdk.core.launch.MoReLauncher;
import com.mobilesorcery.sdk.core.memory.MemoryLowListener;
import com.mobilesorcery.sdk.core.stats.Stats;
import com.mobilesorcery.sdk.profiles.IVendor;
import com.mobilesorcery.sdk.profiles.ProfileDBManager;
import com.mobilesorcery.sdk.ui.internal.LegacyProfileViewOpener;
import com.mobilesorcery.sdk.ui.internal.MemoryLowDialog;
import com.mobilesorcery.sdk.ui.internal.console.IDEProcessConsole;
import com.mobilesorcery.sdk.ui.internal.decorators.ExcludedResourceDecorator;
import com.mobilesorcery.sdk.ui.internal.launch.AutomaticEmulatorLauncherPart;
import com.mobilesorcery.sdk.ui.internal.launch.EmulatorLaunchConfigurationPartProxy;
import com.mobilesorcery.sdk.ui.internal.launch.MoreLauncherPart;
import com.mobilesorcery.sdk.ui.internal.properties.CommandLineBuildStepEditor;
import com.mobilesorcery.sdk.ui.internal.properties.CopyBuildStepEditor;
import com.mobilesorcery.sdk.ui.launch.IEmulatorLaunchConfigurationPart;

/**
 * The activator class controls the plug-in life cycle
 */
public class MosyncUIPlugin extends AbstractUIPlugin implements
		IWindowListener, ISelectionListener,
		IProvider<IProcessConsole, String>, MemoryLowListener,
		IResourceChangeListener, IStartup {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.mobilesorcery.sdk.ui"; //$NON-NLS-1$

	/**
	 * A property indicating the current project has changed
	 */
	public static final String CURRENT_PROJECT_CHANGED = PLUGIN_ID
			+ ":current.project.changed"; //$NON-NLS-1$

	public static final String IMG_OVR_EXCLUDED_RESOURCE = "excl.res"; //$NON-NLS-1$

	static final String PASSWORD_SHOW = "p.show"; //$NON-NLS-1$

	static final String PASSWORD_HIDE = "p.hide"; //$NON-NLS-1$

	static final String COLLAPSE_ALL = "collapse.all"; //$NON-NLS-1$

	static final String EXPAND_ALL = "expand.all"; //$NON-NLS-1$

	public static final String FONT_INFO_TEXT = "font.instr"; //$NON-NLS-1$

	public static final String FONT_DEFAULT_BOLD = "b"; //$NON-NLS-1$

	public static final String FONT_DEFAULT_ITALIC = "i"; //$NON-NLS-1$

	public static final String PHONE_IMAGE = "phone"; //$NON-NLS-1$

	public static final String TARGET_PHONE_IMAGE = "target.phone"; //$NON-NLS-1$

	public static final String IMG_BINARY = "binary"; //$NON-NLS-1$

	public static final String IMG_FILTER = "filter"; //$NON-NLS-1$

	public static final String IMG_LOOKUP = "lookup"; //$NON-NLS-1$
	
	public static final String IMG_BUILD_ONE = "build.one"; //$NON-NLS-1$
	
	public static final String IMG_WIFI = "wifi"; //$NON-NLS-1$

	private final static Object NULL = new Object();

	protected static final long WB_WAIT_TIMEOUT = 45;

	// The shared instance
	private static MosyncUIPlugin plugin;

	private final CopyOnWriteArrayList<ISelectionListener> customSelectionListeners = new CopyOnWriteArrayList<ISelectionListener>();

	private PropertyChangeSupport listeners;

	private boolean listenerAdded;

	private final IdentityHashMap<IWorkbenchWindow, IProject> currentProjects = new IdentityHashMap<IWorkbenchWindow, IProject>();

	private PropertyChangeListener globalListener;

	private HashMap<String, IEmulatorLaunchConfigurationPart> launcherParts = null;

	private final HashMap<Display, FontRegistry> fontRegistries = new HashMap<Display, FontRegistry>();

	private final CountDownLatch workbenchStarted = new CountDownLatch(1);

	@SuppressWarnings("serial")
	private final Cache<String, Object> platformImages = new Cache<String, Object>(
			64) {
		@Override
		protected void onRemoval(String key, Object value) {
			if (value instanceof Image) {
				if (CoreMoSyncPlugin.getDefault().isDebugging()) {
					CoreMoSyncPlugin.trace("Disposed image " + key); //$NON-NLS-1$
				}
				((Image) value).dispose();
			}
		}
	};

	private LegacyProfileViewOpener legacyProfileViewOpener;

	private boolean isUpdating;

	private HashMap<String, IConfigurationElement> buildStepEditorsExtensions;

	/**
	 * The constructor
	 */
	public MosyncUIPlugin() {
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
		plugin = this;

		listeners = new PropertyChangeSupport(this);
		CoreMoSyncPlugin.getDefault().setIDEProcessConsoleProvider(this);
		registerGlobalProjectListener();
		CoreMoSyncPlugin.getLowMemoryManager().addMemoryLowListener(this, 1);
		if (!CoreMoSyncPlugin.isHeadless()) {
			awaitWorkbenchStartup(new IWorkbenchStartupListener() {
				@Override
				public void started(IWorkbench wb) {
					checkConfig(wb);
					initializeCustomActivities();
					askForUsageStatistics(wb);
				}
			});
			legacyProfileViewOpener = new LegacyProfileViewOpener();
			// Do not use addListener here, since it will trigger a deadlock-type
			// error
			if (legacyProfileViewOpener.isActive()) {
				listeners.addPropertyChangeListener(legacyProfileViewOpener);
				MoSyncProject.addGlobalPropertyChangeListener(legacyProfileViewOpener);
			}
		}
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);

		// Explicitly invoke updates
		CoreMoSyncPlugin.getDefault().checkAutoUpdate();
	}

	protected void checkConfig(final IWorkbench wb) {
		if (!MoSyncTool.getDefault().isValid()) {
			wb.getDisplay().syncExec(new Runnable() {
				public void run() {
					MessageDialog dialog = new MessageDialog(wb.getModalDialogShellProvider().getShell(),
							"Fatal error", null,
							MessageFormat.format("The environment variable MOSYNCDIR has not been properly set, or some other serious error has occurred. (Reason: {0})\n\nMoSync will not work properly. Press Ok to quit, or Cancel to continue anyway.",
									MoSyncTool.getDefault().validate()),
							MessageDialog.ERROR,
							new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL },
							0);
					if (dialog.open() == IDialogConstants.OK_ID) {
						wb.close();
					}
				}
			});
		}
	}

	private void initializeLauncherParts() {
		if (launcherParts == null) {
			launcherParts = new HashMap<String, IEmulatorLaunchConfigurationPart>();
			IConfigurationElement[] launcherParts = Platform.getExtensionRegistry()
					.getConfigurationElementsFor(
							IEmulatorLaunchConfigurationPart.EXTENSION_POINT_ID);
			for (int i = 0; i < launcherParts.length; i++) {
				IConfigurationElement launcherPart = launcherParts[i];
				String id = launcherPart.getAttribute("launcher"); //$NON-NLS-1$
				this.launcherParts.put(id,
						new EmulatorLaunchConfigurationPartProxy(launcherPart));
			}
			// Default + auto always present
			this.launcherParts.put(AutomaticEmulatorLauncher.ID,
					new AutomaticEmulatorLauncherPart());
			this.launcherParts.put(MoReLauncher.ID, new MoreLauncherPart());
		}
	}

	private void askForUsageStatistics(final IWorkbench wb) {
		final Stats stats = Stats.getStats();
		if (stats.getSendInterval() == Stats.UNASSIGNED_SEND_INTERVAL) {
			Display display = wb.getDisplay();
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					Shell shell = wb.getActiveWorkbenchWindow().getShell();
					boolean ok = MessageDialog.openQuestion(shell,
							Messages.MosyncUIPlugin_12,
							Messages.MosyncUIPlugin_13);
					stats.setSendInterval(ok ? Stats.DEFAULT_SEND_INTERVAL
							: Stats.DISABLE_SEND);
				}
			});
		}
	}

	public boolean isExampleWorkspace() {
		IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
		File wsFile = wsRoot.getLocation().toFile();
		File exampleWSPath = MoSyncTool.getDefault()
				.getMoSyncExamplesWorkspace().toFile();
		if (exampleWSPath.exists()) {
			boolean isExampleWorkspace = wsFile.equals(exampleWSPath);
			return isExampleWorkspace;
		} else {
			return false;
		}
	}

	private void closeIntro(IIntroManager im) {
		IIntroPart part = im.getIntro();
		im.closeIntro(part);
	}

	public void closeIntro(IWorkbenchSite site) {
		closeIntro(site.getWorkbenchWindow().getWorkbench().getIntroManager());
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
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		CoreMoSyncPlugin.getLowMemoryManager().removeMemoryLowListener(this);
		deregisterGlobalProjectListener();
		disposePlatformImages();
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static MosyncUIPlugin getDefault() {
		return plugin;
	}

	/**
	 * Convenience method for creating a <code>MoSyncProject</code> at the
	 * requested location, or in the workspace if location is <code>null</code>.
	 *
	 * @param project
	 * @param location
	 * @param monitor
	 * @return
	 * @throws CoreException
	 * @deprecated Use
	 *             {@link MoSyncUIPlugin#createNewProject(IProject, URI, IProgressMonitor)}
	 *             instead.
	 */
	@Deprecated
	public static MoSyncProject createProject(IProject project, URI location,
			IProgressMonitor monitor) throws CoreException {
		return createNewProject(project, location, monitor);
	}

	/**
	 * Convenience method for creating a <code>MoSyncProject</code> at the
	 * requested location, or in the workspace if location is <code>null</code>.
	 *
	 * @param project
	 * @param location
	 * @param monitor
	 * @return
	 * @throws CoreException
	 */
	public static MoSyncProject createNewProject(IProject project,
			URI location, IProgressMonitor monitor) throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProjectDescription description = workspace
				.newProjectDescription(project.getName());
		description.setLocationURI(location);

		if (CoreMoSyncPlugin.isHeadless()) {
			project.create(description, monitor);
		} else {
			// Get undo for free.
			CreateProjectOperation op = new CreateProjectOperation(description,
					"Create Project");
			try {
				op.execute(monitor, null);
			} catch (ExecutionException e) {
				if (e.getCause() instanceof CoreException) {
					throw (CoreException) e.getCause();
				} else {
					throw new CoreException(new Status(IStatus.ERROR,
							CoreMoSyncPlugin.PLUGIN_ID, e.getMessage(), e));
				}
			}
		}

		MoSyncNature.addNatureToProject(project, false);
		MoSyncProject.addDefaultResourceFilter(project, new NullProgressMonitor());
		MoSyncProject result = MoSyncProject.create(project);
		result.activateBuildConfigurations();
		int pt = ProfileDBManager.isAvailable() ? MoSyncTool.DEFAULT_PROFILE_TYPE :
			MoSyncTool.LEGACY_PROFILE_TYPE;
		result.setProperty(MoSyncProject.PROFILE_MANAGER_TYPE_KEY,
				PropertyUtil.fromInteger(pt));
		MoSyncProject.addCapabilityFilters(result, new String[0]);
		MoSyncNature.modifyIncludePaths(project);
		// MOSYNC-1735: Set to UTF-8 by default
		project.setDefaultCharset("UTF-8", new SubProgressMonitor(monitor, 1));
		return result;
	}

	private void initProjectChangeListener() {
		if (!listenerAdded) {
			if (PlatformUI.getWorkbench() != null) {
				PlatformUI.getWorkbench().addWindowListener(this);
				listenerAdded = true;

				IWorkbenchWindow[] windows = PlatformUI.getWorkbench()
						.getWorkbenchWindows();
				for (int i = 0; i < windows.length; i++) {
					windows[i].getSelectionService().addSelectionListener(this);
					updateCurrentlySelectedProjectFallback();
					updateCurrentlySelectedProject(windows[i]);
					updateCurrentlySelectedProject(windows[i], null);
				}
			}
		}
	}

	public void addListener(PropertyChangeListener listener) {
		listeners.addPropertyChangeListener(listener);
		updateCurrentlySelectedProject(PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow());
	}

	public void removeListener(PropertyChangeListener listener) {
		listeners.removePropertyChangeListener(listener);
	}

	public MoSyncProject getCurrentlySelectedProject(IWorkbenchWindow window) {
		if (window == null) {
			return null;
		}

		//updateCurrentlySelectedProject(window);
		IProject project = currentProjects.get(window);

		return project == null ? null : MoSyncProject.create(project);
	}

	/**
	 * Sets the current project given a set of "common" views that may have a
	 * selected project.
	 */
	private void updateCurrentlySelectedProjectFallback() {
		updateCurrentlySelectedProjectFromView("org.eclipse.ui.navigator.ProjectExplorer"); //$NON-NLS-1$
	}

	private boolean updateCurrentlySelectedProjectFromView(String viewId) {
		try {
			IWorkbenchWindow window = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow();
			IWorkbenchPage[] pages = window.getPages();
			for (int i = 0; i < pages.length; i++) {
				IViewPart view = pages[i].findView(viewId);
				if (view != null) {
					IResource selectedResource = getResource(view.getSite()
							.getSelectionProvider().getSelection());
					if (selectedResource != null) {
						setCurrentProject(window, selectedResource.getProject());
						return true;
					}
				}
			}
		} catch (Exception e) {
			// Just ignore, we did the best we could.
		}

		return false;
	}

	private void updateCurrentlySelectedProject(final IWorkbenchWindow window) {
		if (isUpdating) {
			return;
		}
		isUpdating = true;
		initProjectChangeListener();

		Runnable r = new Runnable() {
			@Override
			public void run() {
				ISelection selection = window.getSelectionService()
						.getSelection();
				IResource selectedResource = getResource(selection);
				if (selectedResource != null) {
					setCurrentProject(window, selectedResource.getProject());
				}
			}
		};
		try {
			if (window != null) {
				UIUtils.onUiThread(window.getWorkbench().getDisplay(), r, false);
			}
		} finally {
			isUpdating = false;
		}
	}

	private void updateCurrentlySelectedProject(final IWorkbenchWindow window,
			final IEditorPart editor) {
		Runnable r = new Runnable() {
			@Override
			public void run() {
				if (editor != null) {
					IEditorInput input = editor.getEditorInput();
					if (input instanceof IFileEditorInput) {
						IFile file = ResourceUtil.getFile(input);
						if (file != null) {
							setCurrentProject(window, file.getProject());
						}
					}
				} else {
					IEditorPart activeEditor = window.getActivePage()
							.getActiveEditor();
					if (activeEditor != null) {
						updateCurrentlySelectedProject(window, activeEditor);
					}
				}
			}
		};
		UIUtils.onUiThread(window.getWorkbench().getDisplay(), r, true);
	}

	private void setCurrentProject(IWorkbenchWindow window, IProject project) {
		IProject oldProject = currentProjects.get(window);
		if (MoSyncProject.create(project) == null) {
			project = null;
		}
		currentProjects.put(window, project);
		if (oldProject != project) {
			listeners.firePropertyChange(new PropertyChangeEvent(this,
					CURRENT_PROJECT_CHANGED, oldProject, project));
		}
	}

	IResource getResource(ISelection selection) {
		if (selection instanceof TreeSelection) {
			TreePath[] paths = ((TreeSelection) selection).getPaths();
			IResource result = null;
			if (paths != null && paths.length > 0) {
				Object firstSegment = paths[0].getFirstSegment();
				if (firstSegment instanceof IResource) {
					result = (IResource) firstSegment;
				} else if (firstSegment instanceof IAdaptable) {
					result = (IResource) ((IAdaptable) firstSegment)
							.getAdapter(IResource.class);
					if (result == null) {
						ILaunchConfiguration launchConfig = (ILaunchConfiguration) ((IAdaptable) firstSegment)
								.getAdapter(ILaunchConfiguration.class);
						try {
							result = MoSyncBuilder.getProject(launchConfig);
						} catch (CoreException e) {
							// Ignore.
						}
					}
				}

				return result;
			}
		}
		return null;
	}

	@Override
	public void windowClosed(IWorkbenchWindow window) {
		window.getSelectionService().removeSelectionListener(this);
		currentProjects.remove(window);
	}

	@Override
	public void windowActivated(IWorkbenchWindow window) {
		// Ignore.
	}

	@Override
	public void windowDeactivated(IWorkbenchWindow window) {
		// Ignore.
	}

	@Override
	public void windowOpened(IWorkbenchWindow window) {
		window.getSelectionService().addSelectionListener(this);
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (part instanceof IEditorPart) {
			updateCurrentlySelectedProject(part.getSite().getWorkbenchWindow(),
					(IEditorPart) part);
		} else {
			updateCurrentlySelectedProject(part.getSite().getWorkbenchWindow());
		}

		callOtherSelectionListeners(part, selection);
	}

	private void callOtherSelectionListeners(final IWorkbenchPart part,
			final ISelection selection) {
		for (Iterator<ISelectionListener> customSelectionListeners = this.customSelectionListeners
				.iterator(); customSelectionListeners.hasNext();) {
			final ISelectionListener customSelectionListener = customSelectionListeners
					.next();
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void handleException(Throwable exception) {
					log(exception);
				}

				@Override
				public void run() throws Exception {
					customSelectionListener.selectionChanged(part, selection);
				}
			});
		}
	}

	public void registerGlobalProjectListener() {
		globalListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				Object source = event.getSource();
				if (MoSyncProject.BUILD_CONFIGURATION_CHANGED.equals(event
						.getPropertyName())
						|| MoSyncProject.BUILD_CONFIGURATION_SUPPORT_CHANGED
								.equals(event.getPropertyName())
						|| MoSyncProject.EXCLUDE_FILTER_KEY
								.equals(NameSpacePropertyOwner.getKey(event
										.getPropertyName()))) {
					try {
						final ExcludedResourceDecorator dec = (ExcludedResourceDecorator) PlatformUI
								.getWorkbench()
								.getDecoratorManager()
								.getLabelDecorator(ExcludedResourceDecorator.ID);
						if (dec != null) {
							IWorkbenchWindow ww = PlatformUI.getWorkbench()
									.getActiveWorkbenchWindow();
							if (ww != null) {
								Shell shell = ww.getShell();
								if (shell != null) {
									shell.getDisplay().asyncExec(
											new Runnable() {
												@Override
												public void run() {
													dec.updateDecorations();
												}
											});
								}
							}
						}
					} catch (Exception e) {
						CoreMoSyncPlugin.getDefault().log(e);
					}
				}
			}
		};

		MoSyncProject.addGlobalPropertyChangeListener(globalListener);

	}

	private void deregisterGlobalProjectListener() {
		MoSyncProject.removeGlobalPropertyChangeListener(globalListener);
	}

	public void log(Throwable e) {
		getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
	}

	@Override
	public IProcessConsole get(String name) {
		return new IDEProcessConsole(name);
	}

	@Override
	public void initializeImageRegistry(ImageRegistry reg) {
		super.initializeImageRegistry(reg);
		reg.put(IMG_OVR_EXCLUDED_RESOURCE, AbstractUIPlugin
				.imageDescriptorFromPlugin(MosyncUIPlugin.PLUGIN_ID,
						"$nl$/icons/exclude_ovr.png")); //$NON-NLS-1$
		reg.put(PASSWORD_HIDE, AbstractUIPlugin.imageDescriptorFromPlugin(
				MosyncUIPlugin.PLUGIN_ID, "$nl$/icons/hide_pwd.png")); //$NON-NLS-1$
		reg.put(PASSWORD_SHOW, AbstractUIPlugin.imageDescriptorFromPlugin(
				MosyncUIPlugin.PLUGIN_ID, "$nl$/icons/show_pwd.png")); //$NON-NLS-1$
		reg.put(COLLAPSE_ALL, AbstractUIPlugin.imageDescriptorFromPlugin(
				PLUGIN_ID, "$nl$/icons/collapseall.gif")); //$NON-NLS-1$
		reg.put(EXPAND_ALL, AbstractUIPlugin.imageDescriptorFromPlugin(
				PLUGIN_ID, "$nl$/icons/expandall.gif")); //$NON-NLS-1$
		reg.put(PHONE_IMAGE,
				ImageDescriptor.createFromFile(getClass(), "/icons/phone.png")); //$NON-NLS-1$
		reg.put(TARGET_PHONE_IMAGE, ImageDescriptor.createFromFile(getClass(),
				"/icons/phoneTarget.png")); //$NON-NLS-1$
		reg.put(IMG_BINARY,
				ImageDescriptor.createFromFile(getClass(), "/icons/binary.gif")); //$NON-NLS-1$
		reg.put(IMG_FILTER,
				ImageDescriptor.createFromFile(getClass(), "/icons/filter.gif")); //$NON-NLS-1$
		reg.put(IMG_LOOKUP,
				ImageDescriptor.createFromFile(getClass(), "/icons/search.gif")); //$NON-NLS-1$
		reg.put(IMG_BUILD_ONE,
				ImageDescriptor.createFromFile(getClass(), "/icons/build_one.png")); //$NON-NLS-1$
		reg.put(IMG_WIFI,
				ImageDescriptor.createFromFile(getClass(), "/icons/wireless.png")); //$NON-NLS-1$
	}

	public static Image resize(Image original, int width, int height,
			boolean disposeOriginal, boolean keepAspectRatio) {
		Display display = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getShell().getDisplay();
		if (keepAspectRatio) {
			int originalWidth = original.getBounds().width;
			int originalHeight = original.getBounds().height;
			double aspectRatio = (double) originalWidth
					/ (double) originalHeight;
			double newWidth = height * aspectRatio;
			if (newWidth > width && width > 0) {
				double overSizeRatio = newWidth / width;
				newWidth = width;
				height = (int) Math.floor(height / overSizeRatio);
			}
			width = (int) newWidth;
		}
		Image scaled = new Image(display, width, height);
		GC gc = new GC(scaled);
		gc.setAntialias(SWT.ON);
		gc.drawImage(original, 0, 0, original.getBounds().width,
				original.getBounds().height, 0, 0, width, height);
		gc.dispose();

		if (disposeOriginal) {
			original.dispose();
		}

		return scaled;
	}

	public void showHelp(String helpResource, boolean showInExternalBrowser) {
		if (helpResource != null) {
			if (showInExternalBrowser) {
				try {
					URL urlToHelpDoc = FileLocator.find(
							Platform.getBundle("com.mobilesorcery.sdk.help"), //$NON-NLS-1$
							new Path(helpResource), null);
					if (urlToHelpDoc == null) {
						throw new IllegalStateException(
								"Help files not available!");
					}
					URL fileUrlToHelpDoc = FileLocator.toFileURL(urlToHelpDoc);

					PlatformUI.getWorkbench().getBrowserSupport()
							.getExternalBrowser().openURL(fileUrlToHelpDoc);
				} catch (Exception e) {
					org.eclipse.jface.util.Policy.getStatusHandler().show(
							new Status(IStatus.ERROR, PLUGIN_ID,
									"Could not show help", e),
							"Could not show help");
					CoreMoSyncPlugin.getDefault().log(e);
				}
			} else {
				PlatformUI.getWorkbench().getHelpSystem()
						.displayHelpResource(helpResource);
			}
		}
	}

	public String getUserHash() {
		return MoSyncTool.getDefault().getProperty(MoSyncTool.USER_HASH_PROP_2);
	}

	public String getUserHalfHash() {
		String hash = getUserHash();
		if (hash != null) {
			return hash.substring(0, hash.length() / 2);
		}

		return null;
	}

	private void addHalfHash(Map<String, String> request) {
		request.put("hhash", getUserHalfHash()); //$NON-NLS-1$
	}

	// REFACTOR AFTER 3.0.
	public Map<String, String> getVersionParameters() {
		HashMap<String, String> params = new HashMap<String, String>();
		String versionStr = MoSyncTool.getDefault().getVersionInfo(MoSyncTool.BINARY_VERSION);
		// For now we send the same version for all components.
		params.put("db", versionStr); //$NON-NLS-1$
		params.put("sdk", versionStr); //$NON-NLS-1$
		params.put("ide", versionStr); //$NON-NLS-1$
		return params;
	}

	/**
	 * Returns a set of version parameters that may be used by updaters, help
	 * urls, etc, to inform the server about the user's registration status.
	 *
	 * @param hashOnly
	 * @return
	 */
	public Map<String, String> getVersionParameters(boolean hashOnly) {
		HashMap<String, String> params = new HashMap<String, String>();
		if (!hashOnly) {
			params.putAll(getVersionParameters());
		}
		addHalfHash(params);
		params.put("hhash", getUserHalfHash()); //$NON-NLS-1$
		return params;
	}

	private void initializeCustomActivities() {
		boolean enable = Boolean.TRUE.equals(IsExperimentalTester
				.isExperimental());
		boolean disable = Boolean.FALSE.equals(IsExperimentalTester
				.isExperimental());
		if (enable || disable) {
			IWorkbenchActivitySupport activitySupport = PlatformUI
					.getWorkbench().getActivitySupport();
			ICategory category = activitySupport.getActivityManager()
					.getCategory("com.mobilesorcery.activities.experimental"); //$NON-NLS-1$
			if (category != null) {
				HashSet<String> activityIds = new HashSet<String>(
						activitySupport.createWorkingCopy()
								.getEnabledActivityIds());
				for (Object binding : category.getCategoryActivityBindings()) {
					if (enable) {
						activityIds.add(((ICategoryActivityBinding) binding)
								.getActivityId());
					} else {
						activityIds.remove(((ICategoryActivityBinding) binding)
								.getActivityId());
					}
				}
				activitySupport.setEnabledActivityIds(activityIds);
			}
		}
	}

	public IEmulatorLaunchConfigurationPart getEmulatorLauncherPart(String id) {
		initializeLauncherParts();
		return launcherParts.get(id);
	}

	/**
	 * <p>
	 * Returns a standard font used throughout the MoSync IDE. Clients should
	 * <b>not</b> dispose these fonts.
	 * </p>
	 *
	 * @param fontId
	 * @return
	 */
	public Font getFont(String fontId) {
		if (Display.getCurrent() == null) {
			throw new AssertionFailedException("Must be called from UI thread"); //$NON-NLS-1$
		}
		return getFontRegistry(Display.getCurrent()).get(fontId);
	}

	private synchronized FontRegistry getFontRegistry(Display current) {
		FontRegistry registry = fontRegistries.get(current);
		if (registry == null) {
			registry = new FontRegistry(current);
			fontRegistries.put(current, registry);
			initRegistry(registry);
		}

		return registry;
	}

	private void initRegistry(FontRegistry registry) {
		int infoTextSizeDelta = Util.isMac() ? -2 : -1;
		registry.put(FONT_INFO_TEXT, UIUtils.modifyFont((FontData[]) null,
				SWT.DEFAULT, infoTextSizeDelta));
		registry.put(FONT_DEFAULT_BOLD,
				UIUtils.modifyFont((FontData[]) null, SWT.BOLD, 0));
		registry.put(FONT_DEFAULT_ITALIC,
				UIUtils.modifyFont((FontData[]) null, SWT.ITALIC, 0));
	}

	@Override
	public void memoryUsageLow() {
		final Display d = PlatformUI.getWorkbench().getDisplay();
		MemoryLowDialog.open(d);
	}

	public Image getPlatformImage(IVendor vendor, Point imageSize) {
		String key = imageSize + Messages.MosyncUIPlugin_0 + vendor.getName();
		Object image = platformImages.get(key);
		if (image == null) {
			Object addImage = NULL;
			ImageDescriptor vendorImageDesc = vendor.getIcon();
			if (vendorImageDesc != null) {
				addImage = vendorImageDesc.createImage();
				if (imageSize != null) {
					addImage = MosyncUIPlugin.resize((Image) addImage,
							imageSize.x, imageSize.y, true, true);
				}
			}

			platformImages.put(key, addImage);
			image = addImage;

			if (CoreMoSyncPlugin.getDefault().isDebugging()) {
				CoreMoSyncPlugin.trace("Allocated image " + key); //$NON-NLS-1$
			}
		}

		return (Image) (image == NULL ? null : image);
	}

	public void disposePlatformImages() {
		platformImages.clear();
	}

	@Override
	public void resourceChanged(final IResourceChangeEvent event) {
		if (!PlatformUI.isWorkbenchRunning()) {
			return;
		}
		UIUtils.onUiThread(PlatformUI.getWorkbench().getDisplay(),
				new Runnable() {
					@Override
					public void run() {
						if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
							if (event.getSource() instanceof IProject || event.getSource() instanceof IWorkspace) {
								updateCurrentlySelectedProject(PlatformUI
										.getWorkbench()
										.getActiveWorkbenchWindow());
							}
						}
					}
				}, true);
	}

    /**
     * <p>Waits for the workbench to startup (if <code>null</code> is passed
     * as the listener) or asynchronously sends a message to the listener
     * once it's started.</p>
     * @param listener The listener to receive events, or <code>null</code> if this
     * method should block until the workbench has started.
     */
    public void awaitWorkbenchStartup(final IWorkbenchStartupListener listener) {
    	TimerTask awaitRunnable = new TimerTask() {
			@Override
			public void run() {
				try {
					if (!isWorkbenchStarted()) {
						workbenchStarted.await(WB_WAIT_TIMEOUT, TimeUnit.SECONDS);
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				if (listener != null) {
					listener.started(PlatformUI.getWorkbench());
				}
			}
		};
    	if (listener == null) {
    		awaitRunnable.run();
    	} else {
    		Thread t = new Thread(awaitRunnable);
    		t.setDaemon(true);
    		t.start();
    	}
    }

	@Override
	public void earlyStartup() {
		workbenchStarted.countDown();
	}

	public boolean isWorkbenchStarted() {
		return PlatformUI.isWorkbenchRunning() && !PlatformUI.getWorkbench().isStarting() && !PlatformUI.getWorkbench().isClosing();
	}

	public IBuildStepEditor createBuildStepEditor(MoSyncProject project, IBuildStepFactory factory) {
		if (factory == null) {
			return null;
		}
		
		IBuildStepEditor editor = null;
		
		if (factory instanceof CommandLineBuildStep.Factory) {
			editor = new CommandLineBuildStepEditor();
		} else if (factory instanceof CopyBuildResultBuildStep.Factory) {
			editor = new CopyBuildStepEditor();
		} else {
			editor = createBuildStepEditorExtension(factory.getId());
		}
		
		if (editor != null) {
			editor.setProject(project);
			editor.setBuildStepFactory(factory);
		}
		
		return editor;
	}
	
	private IBuildStepEditor createBuildStepEditorExtension(String id) {
		if (buildStepEditorsExtensions == null) {
			buildStepEditorsExtensions = new HashMap<String, IConfigurationElement>();
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			IConfigurationElement[] elements = registry
					.getConfigurationElementsFor(IBuildStepEditor.EXTENSION_ID);
			for (IConfigurationElement element : elements) {
					String extId = element.getAttribute("id");
					buildStepEditorsExtensions.put(extId,  element);
			}
		}
		try {
			IConfigurationElement ext = buildStepEditorsExtensions.get(id);
			if (ext != null) {
				return (IBuildStepEditor) ext.createExecutableExtension("implementation");
			}
		} catch (CoreException e) {
			CoreMoSyncPlugin.getDefault().log(e);
		}
		return null;
	}

}
