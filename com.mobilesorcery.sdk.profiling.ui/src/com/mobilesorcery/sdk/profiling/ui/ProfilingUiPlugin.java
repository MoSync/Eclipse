package com.mobilesorcery.sdk.profiling.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class ProfilingUiPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.mobilesorcery.sdk.profiling.ui";

    public static final String METHOD_IMG = "method";

    public static final String CALL_TREE_TAB_IMG = "call.tree.tab.img";

    public static final String HOTSPOTS_TAB_IMG = "hotspots.tab.img";
    
    public static final String EXPORT_SESSION_IMG = "export.session";
    
   	// The shared instance
	private static ProfilingUiPlugin plugin;
	
	/**
	 * The constructor
	 */
	public ProfilingUiPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static ProfilingUiPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public void initializeImageRegistry(ImageRegistry reg) {
        super.initializeImageRegistry(reg);
        reg.put(METHOD_IMG, AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, "$nl$/icons/methods_co.gif"));
        reg.put(CALL_TREE_TAB_IMG, AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, "$nl$/icons/call_tree.gif"));
        reg.put(HOTSPOTS_TAB_IMG, AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, "$nl$/icons/hotspots.gif"));
        reg.put(EXPORT_SESSION_IMG, AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, "$nl$/icons/export.gif"));
	}
}
