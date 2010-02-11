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
package com.mobilesorcery.sdk.profiles.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.mobilesorcery.sdk.profiles.ui"; //$NON-NLS-1$

    public static final String PHONE_IMAGE = "phone"; //$NON-NLS-1$

    public static final String TARGET_PHONE_IMAGE = "target.phone"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

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
	public static Activator getDefault() {
		return plugin;
	}

	public void initializeImageRegistry(ImageRegistry reg) {
        reg.put(PHONE_IMAGE, ImageDescriptor.createFromFile(getClass(), "/icons/phone.png")); //$NON-NLS-1$
        reg.put(TARGET_PHONE_IMAGE, ImageDescriptor.createFromFile(getClass(), "/icons/phoneTarget.png")); //$NON-NLS-1$
	}
	
	public static Image resize(Image original, int width, int height, boolean disposeOriginal) {
	    Display display = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getDisplay();

	    Image scaled = new Image(display, width, height);	    
	    GC gc = new GC(scaled);
	    gc.setAntialias(SWT.ON);
        gc.drawImage(original, 0, 0, original.getBounds().width, original.getBounds().height, 0, 0, width, height);
        gc.dispose();
        
        if (disposeOriginal) {
            original.dispose();
        }
        
        return scaled;
	}

}
