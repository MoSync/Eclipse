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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.ICategory;
import org.eclipse.ui.activities.ICategoryActivityBinding;
import org.eclipse.ui.activities.IWorkbenchActivitySupport;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IProcessConsole;
import com.mobilesorcery.sdk.core.IProvider;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.NameSpacePropertyOwner;
import com.mobilesorcery.sdk.core.launch.IEmulatorLauncher;
import com.mobilesorcery.sdk.core.launch.MoReLauncher;
import com.mobilesorcery.sdk.internal.launch.EmulatorLauncherProxy;
import com.mobilesorcery.sdk.ui.internal.console.IDEProcessConsole;
import com.mobilesorcery.sdk.ui.internal.decorators.ExcludedResourceDecorator;
import com.mobilesorcery.sdk.ui.internal.launch.EmulatorLaunchConfigurationPartProxy;
import com.mobilesorcery.sdk.ui.internal.launch.MoreLauncherPart;
import com.mobilesorcery.sdk.ui.launch.IEmulatorLaunchConfigurationPart;

/**
 * The activator class controls the plug-in life cycle
 */
public class MosyncUIPlugin extends AbstractUIPlugin implements IWindowListener, ISelectionListener, IProvider<IProcessConsole, String> {

    // The plug-in ID
    public static final String PLUGIN_ID = "com.mobilesorcery.sdk.ui";

    /**
     * A property indicating the current project has changed
     */
    public static final String CURRENT_PROJECT_CHANGED = PLUGIN_ID + ":current.project.changed";

    public static final String IMG_OVR_EXCLUDED_RESOURCE = "excl.res";

    static final String PASSWORD_SHOW = "p.show";

    static final String PASSWORD_HIDE = "p.hide";

    static final String COLLAPSE_ALL = "collapse.all";
    
    static final String EXPAND_ALL = "expand.all";

	public static final String FONT_INFO_TEXT = "font.instr";

	public static final String FONT_DEFAULT_BOLD = "b";
	
	public static final String FONT_DEFAULT_ITALIC = "i";

    // The shared instance
    private static MosyncUIPlugin plugin;

    private CopyOnWriteArrayList<ISelectionListener> customSelectionListeners = new CopyOnWriteArrayList<ISelectionListener>();

    private PropertyChangeSupport listeners;

    private boolean listenerAdded;

    private IdentityHashMap<IWorkbenchWindow, IProject> currentProjects = new IdentityHashMap<IWorkbenchWindow, IProject>();

    private PropertyChangeListener globalListener;

	private HashMap<String, IEmulatorLaunchConfigurationPart> launcherParts = new HashMap<String, IEmulatorLaunchConfigurationPart>();

	private HashMap<Display, FontRegistry> fontRegistries = new HashMap<Display, FontRegistry>();

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
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;

        listeners = new PropertyChangeSupport(this);
        CoreMoSyncPlugin.getDefault().setIDEProcessConsoleProvider(this);
        registerGlobalProjectListener();
        initializeCustomActivities();
        initializeLauncherParts();
    }

    private void initializeLauncherParts() {
    	IConfigurationElement[] launcherParts = Platform.getExtensionRegistry().getConfigurationElementsFor(IEmulatorLaunchConfigurationPart.EXTENSION_POINT_ID);
    	for (int i = 0; i < launcherParts.length; i++) {
    		IConfigurationElement launcherPart = launcherParts[i];
    		String id = launcherPart.getAttribute("launcher");
    		this.launcherParts.put(id, new EmulatorLaunchConfigurationPartProxy(launcherPart));
    	}
    	// Default always present
    	this.launcherParts.put(MoReLauncher.ID, new MoreLauncherPart());
	}

	public boolean isExampleWorkspace() {
        IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
        File wsFile = wsRoot.getLocation().toFile();
        File exampleWSPath = MoSyncTool.getDefault().getMoSyncExamplesWorkspace().toFile();
        if(exampleWSPath.exists()) {
            boolean isExampleWorkspace = wsFile.equals(exampleWSPath);
            return isExampleWorkspace;
        }
        else {
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
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
        deregisterGlobalProjectListener();
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
     */
    public static MoSyncProject createProject(IProject project, URI location, IProgressMonitor monitor) throws CoreException {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProjectDescription description = workspace.newProjectDescription(project.getName());
        description.setLocationURI(location);

        CreateProjectOperation op = new CreateProjectOperation(description, "Create Project");
        try {
            op.execute(monitor, null);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof CoreException) {
                throw (CoreException) e.getCause();
            } else {
                throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, e.getMessage(), e));
            }
        }

        MoSyncProject.addNatureToProject(project);
        MoSyncProject result = MoSyncProject.create(project);
        result.activateBuildConfigurations();
        return result;
    }

    private void initProjectChangeListener() {
        if (!listenerAdded) {
            if (PlatformUI.getWorkbench() != null) {
                PlatformUI.getWorkbench().addWindowListener(this);
                listenerAdded = true;

                IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
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
        updateCurrentlySelectedProject(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
    }

    public void removeListener(PropertyChangeListener listener) {
        listeners.removePropertyChangeListener(listener);
    }

    public MoSyncProject getCurrentlySelectedProject(IWorkbenchWindow window) {
        if (window == null) {
            return null;
        }

        updateCurrentlySelectedProject(window);
        IProject project = currentProjects.get(window);

        return project == null ? null : MoSyncProject.create(project);
    }

    /**
     * Sets the current project given a set of "common" views that may have a
     * selected project.
     */
    private void updateCurrentlySelectedProjectFallback() {
        updateCurrentlySelectedProjectFromView("org.eclipse.ui.navigator.ProjectExplorer");
    }

    private boolean updateCurrentlySelectedProjectFromView(String viewId) {
        try {
            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            IWorkbenchPage[] pages = window.getPages();
            for (int i = 0; i < pages.length; i++) {
                IViewPart view = pages[i].findView(viewId);
                if (view != null) {
                    IResource selectedResource = getResource(view.getSite().getSelectionProvider().getSelection());
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
        initProjectChangeListener();

        window.getWorkbench().getDisplay().asyncExec(new Runnable() {
            public void run() {
                ISelection selection = window.getSelectionService().getSelection();
                IResource selectedResource = getResource(selection);
                if (selectedResource != null) {
                    setCurrentProject(window, selectedResource.getProject());
                }
            }
        });
    }

    private void updateCurrentlySelectedProject(final IWorkbenchWindow window, final IEditorPart editor) {
        window.getWorkbench().getDisplay().asyncExec(new Runnable() {
            public void run() {
                if (editor != null) {
                    IEditorInput input = editor.getEditorInput();
                    if (input instanceof IFileEditorInput) {
                        IFile file = ((IFileEditorInput) input).getFile();
                        if (file != null) {
                            setCurrentProject(window, file.getProject());
                        }
                    }
                } else {
                    IEditorPart activeEditor = window.getActivePage().getActiveEditor();
                    if (activeEditor != null) {
                        updateCurrentlySelectedProject(window, activeEditor);
                    }
                }
            }
        });
    }

    private void setCurrentProject(IWorkbenchWindow window, IProject project) {
        IProject oldProject = currentProjects.get(window);
        currentProjects.put(window, project);
        listeners.firePropertyChange(new PropertyChangeEvent(this, CURRENT_PROJECT_CHANGED, oldProject, project));
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
                    result = (IResource) ((IAdaptable) firstSegment).getAdapter(IResource.class);
                    if (result == null) {
                        ILaunchConfiguration launchConfig = (ILaunchConfiguration) ((IAdaptable) firstSegment).getAdapter(ILaunchConfiguration.class);
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

    public void windowClosed(IWorkbenchWindow window) {
        window.getSelectionService().removeSelectionListener(this);
        currentProjects.remove(window);
    }

    public void windowActivated(IWorkbenchWindow window) {
        // Ignore.
    }

    public void windowDeactivated(IWorkbenchWindow window) {
        // Ignore.
    }

    public void windowOpened(IWorkbenchWindow window) {
        window.getSelectionService().addSelectionListener(this);
    }

    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if (part instanceof IEditorPart) {
            updateCurrentlySelectedProject(part.getSite().getWorkbenchWindow(), (IEditorPart) part);
        } else {
            updateCurrentlySelectedProject(part.getSite().getWorkbenchWindow());
        }

        callOtherSelectionListeners(part, selection);
    }

    private void callOtherSelectionListeners(final IWorkbenchPart part, final ISelection selection) {
        for (Iterator<ISelectionListener> customSelectionListeners = this.customSelectionListeners.iterator(); customSelectionListeners.hasNext();) {
            final ISelectionListener customSelectionListener = customSelectionListeners.next();
            SafeRunner.run(new ISafeRunnable() {
                public void handleException(Throwable exception) {
                    log(exception);
                }

                public void run() throws Exception {
                    customSelectionListener.selectionChanged(part, selection);
                }
            });
        }
    }

    public void registerGlobalProjectListener() {
        globalListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                Object source = event.getSource();
                if (MoSyncProject.BUILD_CONFIGURATION_CHANGED.equals(event.getPropertyName())
                        || MoSyncProject.BUILD_CONFIGURATION_SUPPORT_CHANGED.equals(event.getPropertyName())
                        || MoSyncProject.EXCLUDE_FILTER_KEY.equals(NameSpacePropertyOwner.getKey(event.getPropertyName()))) {
                    try {
                        final ExcludedResourceDecorator dec = (ExcludedResourceDecorator) PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator(
                                ExcludedResourceDecorator.ID);
                        if (dec != null) {
                            IWorkbenchWindow ww = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                            if (ww != null) {
                                Shell shell = ww.getShell();
                                if (shell != null) {
                                    shell.getDisplay().asyncExec(new Runnable() {
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

    public IProcessConsole get(String name) {
        return new IDEProcessConsole(name);
    }

    public void initializeImageRegistry(ImageRegistry reg) {
        super.initializeImageRegistry(reg);
        reg.put(IMG_OVR_EXCLUDED_RESOURCE, AbstractUIPlugin.imageDescriptorFromPlugin(MosyncUIPlugin.PLUGIN_ID, "$nl$/icons/exclude_ovr.png"));
        reg.put(PASSWORD_HIDE, AbstractUIPlugin.imageDescriptorFromPlugin(MosyncUIPlugin.PLUGIN_ID, "$nl$/icons/hide_pwd.png"));
        reg.put(PASSWORD_SHOW, AbstractUIPlugin.imageDescriptorFromPlugin(MosyncUIPlugin.PLUGIN_ID, "$nl$/icons/show_pwd.png"));
        reg.put(COLLAPSE_ALL, AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, "$nl$/icons/collapseall.gif"));
        reg.put(EXPAND_ALL, AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, "$nl$/icons/expandall.gif"));
    }

    public void showHelp(String helpResource, boolean showInExternalBrowser) {
        if (helpResource != null) {
            if (showInExternalBrowser) {
                try {
                    URL urlToHelpDoc = FileLocator.find(Platform.getBundle("com.mobilesorcery.sdk.help"), new Path(helpResource), null);
                    URL fileUrlToHelpDoc = FileLocator.toFileURL(urlToHelpDoc);
                    
                    PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(fileUrlToHelpDoc);
                } catch (Exception e) {
                    CoreMoSyncPlugin.getDefault().log(e);
                }
            } else {
                PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(helpResource);  
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
    
    /**
     * Returns a set of version parameters that may be used by
     * updaters, help urls, etc, to inform the server about the
     * user's registration status.
     * @param hashOnly
     * @return
     */
    public Map<String, String> getVersionParameters(boolean hashOnly) {
        HashMap<String, String> params = new HashMap<String, String>();
        if (!hashOnly) {
            int version = MoSyncTool.getDefault().getCurrentBinaryVersion();
            String versionStr = Integer.toString(version);
            // For now we send the same version for all components.
            params.put("db", versionStr);
            params.put("sdk", versionStr);
            params.put("ide", versionStr);
        }
        addHalfHash(params);
        params.put("hhash", getUserHalfHash());
        return params;
    }
    
    private void initializeCustomActivities() {
    	boolean enable = Arrays.asList(Platform.getApplicationArgs()).contains("-experimental:enable");
    	boolean disable = Arrays.asList(Platform.getApplicationArgs()).contains("-experimental:disable");
    	
    	if (enable || disable) {
	    	IWorkbenchActivitySupport activitySupport = PlatformUI.getWorkbench().getActivitySupport();
	    	ICategory category = activitySupport.getActivityManager().getCategory("com.mobilesorcery.activities.experimental");
	    	if (category != null) {
	    		HashSet<String> activityIds = new HashSet<String>(activitySupport.createWorkingCopy().getEnabledActivityIds());
	    		for (Object binding : category.getCategoryActivityBindings()) {
	    			if (enable) {
	    				activityIds.add(((ICategoryActivityBinding) binding).getActivityId());
	    			} else {
	    				activityIds.remove(((ICategoryActivityBinding) binding).getActivityId());
	    			}
	    		}
	    		activitySupport.setEnabledActivityIds(activityIds);	
	    	}
    	}
    }

	public IEmulatorLaunchConfigurationPart getEmulatorLauncherPart(String id) {
		return launcherParts.get(id);
	}
    
	/**
	 * <p>Returns a standard font used throughtout the MoSync IDE.
	 * Clients should <b>not</b> dispose these fonts.</p>
	 * @param fontId
	 * @return
	 */
	public Font getFont(String fontId) {
		if (Display.getCurrent() == null) {
			throw new AssertionFailedException("Must be called from UI thread");
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
		registry.put(FONT_INFO_TEXT, UIUtils.modifyFont((FontData[]) null, SWT.DEFAULT, -1));
		registry.put(FONT_DEFAULT_BOLD, UIUtils.modifyFont((FontData[]) null, SWT.BOLD, 0));
		registry.put(FONT_DEFAULT_ITALIC, UIUtils.modifyFont((FontData[]) null, SWT.ITALIC, 0));
	}

}
