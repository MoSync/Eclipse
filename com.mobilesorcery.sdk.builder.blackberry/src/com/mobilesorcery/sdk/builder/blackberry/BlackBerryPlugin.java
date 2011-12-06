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

	private final ArrayList<JDE> jdes = new ArrayList<JDE>();

	private final ArrayList<Simulator> simulators = new ArrayList<Simulator>();

	/**
	 * The constructor
	 */
	public BlackBerryPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		initJDEs(JDE.TYPE_DEV_TOOLS);
		initJDEs(JDE.TYPE_SIMULATOR);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
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

	public List<JDE> getJDEs(int type) {
		return new ArrayList<JDE>(internalGetJDEs(type));
	}

	private ArrayList internalGetJDEs(int type) {
		return type == JDE.TYPE_DEV_TOOLS ? jdes : simulators;
	}

	/**
	 * Returns a JDE that is compatible with a specific version.
	 * @param version
	 * @return
	 */
	public JDE getCompatibleJDE(Version version, boolean strict) {
		return getCompatibleTool(jdes, version, strict);
	}

	/**
	 * Returns a JDE that is compatible with a specific version.
	 * @param version
	 * @return
	 */
	public Simulator getCompatibleSimulator(Version version, boolean strict) {
		return getCompatibleTool(simulators, version, strict);
	}

	private <ToolType extends JDE> ToolType getCompatibleTool(List<ToolType> tools, Version version, boolean strict) {
		// We usually have only 2-5 JDEs/Simulators installed...
		ToolType bestMatch = null;
		for (ToolType tool : tools) {
			Version toolVersion = tool.getVersion();
			Version bestMatchVersion = bestMatch == null ? null : bestMatch.getVersion();
			boolean isCompatible = toolVersion != null && !toolVersion.isNewer(version);
			if ((!strict || isCompatible) && (bestMatchVersion == null || bestMatchVersion.isNewer(bestMatch.getVersion()))) {
				bestMatch = tool;
			}
		}

		return bestMatch;
	}

	/**
	 * This method is internal to the BlackBerry plugin.
	 * @param newJDEs
	 */
	public void setJDEs(int type, List<JDE> newJDEs) {
		setJDEs(type, newJDEs, true);
	}

	private void setJDEs(int type, List<JDE> newJDEs, boolean doStore) {
		ArrayList<JDE> toolList = internalGetJDEs(type);
		toolList.clear();
		toolList.addAll(newJDEs);
		if (doStore) {
			storeJDEs(type);
		}
	}

	private void initJDEs(int type) {
		int ix = 1;
		IPreferenceStore prefs = getPreferenceStore();
		boolean existsJDE = true;
		ArrayList<JDE> jdes = new ArrayList<JDE>();
		String prefix = getPrefix(type);
		while (existsJDE) {
			String jdeLocation = prefs.getString(prefix + ".location" + ix);
			String jdeVersion = prefs.getString(prefix + ".version" + ix);
			existsJDE = prefs.contains(prefix + ".location" + ix);
			ix++;
			if (existsJDE) {
				jdes.add(JDE.create(type, new Path(jdeLocation), new Version(jdeVersion)));
			}
		}

		setJDEs(type, jdes, false);
	}

	private void storeJDEs(int type) {
		int ix = 1;
		IPreferenceStore prefs = getPreferenceStore();
		String prefix = getPrefix(type);
		for (Object o : internalGetJDEs(type)) {
			JDE jde = (JDE) o;
			IPath jdeLocation = jde.getLocation();
			Version jdeVersion = jde.getVersion();
			if (jdeLocation != null && jdeVersion != null) {
				prefs.putValue(prefix + ".location" + ix, jdeLocation.toPortableString());
				prefs.putValue(prefix + ".version" + ix, jdeVersion.asCanonicalString());
				ix++;
			}
		}
	}

	private String getPrefix(int toolType) {
		return toolType == JDE.TYPE_DEV_TOOLS ? "JDE" : "sim";
	}

}
