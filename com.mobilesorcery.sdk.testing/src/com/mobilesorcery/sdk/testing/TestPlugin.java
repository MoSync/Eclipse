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
package com.mobilesorcery.sdk.testing;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.testing.internal.ui.UnittestView;

/**
 * The activator class controls the plug-in life cycle
 */
public class TestPlugin extends AbstractUIPlugin implements ITestSessionListener {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.mobilesorcery.sdk.testing";

	public static final String ERROR_IMAGE = "error.image";
	public static final String SUCCESS_IMAGE = "success.image";
	
	public static final String TEST_IMAGE = "test.image";
	public static final String TEST_OK_IMAGE = "test.ok.image";
	public static final String TEST_ERROR_IMAGE = "test.error.image";
	public static final String TEST_RUNNING_IMAGE = "test.running.image";
	public static final String TEST_SUITE_IMAGE = "test.suite.image";
	public static final String TEST_SUITE_OK_IMAGE = "test.suite.ok.image";
	public static final String TEST_SUITE_ERROR_IMAGE = "test.suite.error.image";
	public static final String TEST_SUITE_RUNNING_IMAGE = "test.suite.running.image";

    public static final String TEST_BUILD_CONFIGURATION_TYPE = "Test";

	// The shared instance
	private static TestPlugin plugin;
	
	/**
	 * The constructor
	 */
	public TestPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		TestManager.getInstance().addSessionListener(this);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		TestManager.getInstance().removeSessionListener(this);
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static TestPlugin getDefault() {
		return plugin;
	}

	public void initializeImageRegistry(ImageRegistry reg) {
		super.initializeImageRegistry(reg);
		addImage(reg, ERROR_IMAGE, "icons/error_ovr.gif");
		addImage(reg, SUCCESS_IMAGE, "icons/error_ovr.gif");
		addImage(reg, TEST_IMAGE, "icons/test.gif");
		addImage(reg, TEST_OK_IMAGE, "icons/testok.gif");
		addImage(reg, TEST_ERROR_IMAGE, "icons/testerr.gif");
		addImage(reg, TEST_RUNNING_IMAGE, "icons/testrun.gif");
		addImage(reg, TEST_SUITE_IMAGE, "icons/tsuite.gif");
		addImage(reg, TEST_SUITE_OK_IMAGE, "icons/tsuiteok.gif");
		addImage(reg, TEST_SUITE_ERROR_IMAGE, "icons/tsuiteerror.gif");
		addImage(reg, TEST_SUITE_RUNNING_IMAGE, "icons/tsuiterun.gif");
	}

	private void addImage(ImageRegistry reg, String imageKey, String imageLocation) {
		reg.put(imageKey, ImageDescriptor.createFromURL(FileLocator.find(getBundle(), new Path(imageLocation), null)));		
	}
	
	public void handleEvent(TestSessionEvent event) {
		if (event.type == TestSessionEvent.SESSION_STARTED) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					try {
						PlatformUI.getWorkbench().getActiveWorkbenchWindow()
								.getActivePage().showView(UnittestView.ID);
					} catch (PartInitException e) {
						// Ignore.
					}
				}
			});
		}
	}

}
