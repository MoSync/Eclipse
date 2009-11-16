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
package com.mobilesorcery.sdk.ui.editors.resources;

import org.eclipse.jface.text.rules.IPartitionTokenScanner;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.mobilesorcery.sdk.ui.editors.resources";

	// The shared instance
	private static Activator plugin;

	private IPartitionTokenScanner resourceFilePartitionScanner;

	private SyntaxColorPreferenceManager syntaxManager;

	/**
	 * The constructor
	 */
	public Activator() {
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
		resourceFilePartitionScanner = new ResourceFilePartitionScanner();
		syntaxManager = new SyntaxColorPreferenceManager(getPreferenceStore(), createUninitializedSyntaxColoringPreferences());
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
		ColorManager.disposeAll();
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

	public IPartitionTokenScanner getResourceFilePartitionScanner() {
		return resourceFilePartitionScanner;
	}

	public ColorManager getColorManager(Display display) {
		return ColorManager.getColorManager(display);
		/*if (colorManager == null) {
			colorManager = ColorManager.getColorManager(display, true);
			colorManager.loadColorsFromPreferences(getPreferenceStore(), new String[] {
				ResourcesFileScanner.COMMENT_COLOR + SyntaxColoringPreference.FG_SUFFIX,
				ResourcesFileScanner.COMMENT_COLOR + SyntaxColoringPreference.BG_SUFFIX,
				ResourcesFileScanner.DIRECTIVE_COLOR + SyntaxColoringPreference.FG_SUFFIX,
				ResourcesFileScanner.DIRECTIVE_COLOR + SyntaxColoringPreference.BG_SUFFIX
			});
		}
		
		return colorManager;*/
	}

	public SyntaxColorPreferenceManager getSyntaxColorPreferenceManager() {
		return syntaxManager;
	}
	
	public SyntaxColoringPreference[] createUninitializedSyntaxColoringPreferences() {		
		SyntaxColoringPreference[] result = new SyntaxColoringPreference[] {
			new SyntaxColoringPreference(ResourcesFileScanner.COMMENT_COLOR, "Comments"),
			new SyntaxColoringPreference(ResourcesFileScanner.DIRECTIVE_COLOR, "Directives"),
			new SyntaxColoringPreference(ResourcesFileScanner.DEFAULT_TEXT_COLOR, "Default Text Color"),
			new SyntaxColoringPreference(ResourcesFileScanner.STRING_COLOR, "String Literals"),
		};
		
		return result;
	}
}
