package com.mobilesorcery.sdk.builder.blackberry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.core.Version;

/**
 * The activator class controls the plug-in life cycle
 */
public class BlackBerryPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.mobilesorcery.sdk.builder.blackberry"; //$NON-NLS-1$

	public static final String EXTERNAL_SDK_PATH = "external.sdk";

	public static final String BLACKBERRY_SIGNING_INFO = PLUGIN_ID + "sign";

	public static final String PROPERTY_SHOULD_SIGN = PLUGIN_ID + "do.sign";

	public static final String SDK_PATH = PLUGIN_ID + "sdk.path";

	// The shared instance
	private static BlackBerryPlugin plugin;

	private ArrayList<JDE> jdes = new ArrayList<JDE>();
	
	/**
	 * The constructor
	 */
	public BlackBerryPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		initJDEs();
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
	public static BlackBerryPlugin getDefault() {
		return plugin;
	}
	
	public List<JDE> getJDEs() {
		return new ArrayList(jdes);
	}
	
	/**
	 * Returns a JDE that is compatible with a specific version.
	 * @param version
	 * @return
	 */
	public JDE getCompatibleJDE(Version version) {
		// We usually have only 2-5 JDEs installed...
		JDE bestMatch = null;
		for (JDE jde : jdes) {
			Version jdeVersion = jde.getVersion();
			Version bestMatchVersion = bestMatch == null ? null : bestMatch.getVersion();
			boolean isCompatible = jdeVersion != null && !jdeVersion.isNewer(version);
			if (isCompatible && (bestMatchVersion == null || bestMatchVersion.isNewer(version))) {
				bestMatch = jde;
			}
		}
		
		return bestMatch;
	}

	/**
	 * This method is internal to the BlackBerry plugin.
	 * @param newJDEs
	 */
	public void setJDEs(List<JDE> newJDEs) {
		setJDEs(newJDEs, true);
	}
	
	private void setJDEs(List<JDE> newJDEs, boolean doStore) {
		this.jdes = new ArrayList<JDE>(newJDEs);
		if (doStore) {
			storeJDEs();
		}
	}

	private void initJDEs() {
		int ix = 1;
		IPreferenceStore prefs = getPreferenceStore();
		boolean existsJDE = true;
		ArrayList<JDE> jdes = new ArrayList<JDE>();
		while (existsJDE) {
			String jdeLocation = prefs.getString("JDE.location" + ix);
			String jdeVersion = prefs.getString("JDE.version" + ix);
			existsJDE = prefs.contains("JDE.location" + ix);
			ix++;
			if (existsJDE) {
				jdes.add(new JDE(new Path(jdeLocation), new Version(jdeVersion)));
			}
		}
		
		setJDEs(jdes, false);
	}
	
	private void storeJDEs() {
		int ix = 1;
		IPreferenceStore prefs = getPreferenceStore();
		for (JDE jde : jdes) {
			IPath jdeLocation = jde.getLocation();
			Version jdeVersion = jde.getVersion();
			if (jdeLocation != null && jdeVersion != null) {
				prefs.putValue("JDE.location" + ix, jdeLocation.toPortableString());
				prefs.putValue("JDE.version" + ix, jdeVersion.asCanonicalString());
				ix++;
			}
		}
	}

}
