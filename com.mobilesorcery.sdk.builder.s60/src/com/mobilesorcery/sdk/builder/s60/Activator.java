package com.mobilesorcery.sdk.builder.s60;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "com.mobilesorcery.sdk.builder.s60"; //$NON-NLS-1$

    // The shared instance
    private static Activator plugin;

    private DefaultKeyInitializer defaultKeyInitializer;

    /**
     * The constructor
     */
    public Activator() {
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) throws Exception {
        super.start(context);
        initDefaultKeys();
        plugin = this;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        defaultKeyInitializer.dispose();
        super.stop(context);
    }

    private void initDefaultKeys() {
        defaultKeyInitializer = new DefaultKeyInitializer();
        defaultKeyInitializer.startInitializerJob();
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

}
