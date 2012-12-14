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
package com.mobilesorcery.sdk.core;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.IVendor;
import com.mobilesorcery.sdk.profiles.LegacyProfileManager;
import com.mobilesorcery.sdk.profiles.ProfileDBManager;
import com.mobilesorcery.sdk.profiles.ProfileParser;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class MoSyncTool {

	public final static String CONSTANT_PREFIX = "MA_PROF_CONST_";

	public static final String UNVERSIONED = "";

	/**
	 * A filter for excluding constants from the constants.
	 */
	public final static Filter<String> INCLUDE_CONSTANTS_FILTER = new Filter<String>() {
		@Override
		public boolean accept(String obj) {
			return obj != null && obj.startsWith(CONSTANT_PREFIX);
		}
	};

	public final static String MOSYNC_HOME_PREF = "mosync.home";

	public static final String MO_SYNC_HOME_FROM_ENV_PREF = "mosync.home.env";

	public static final String AUTO_UPDATE_PREF = "mosync.auto.update";

	public static final Filter<String> EXCLUDE_CONSTANTS_FILTER = INCLUDE_CONSTANTS_FILTER
			.inverse();

	private static final String MOSYNC_ENV_VAR = "MOSYNCDIR";

	/**
	 * @deprecated This property is obsolete since the 'new' update/registration
	 *             process
	 */
	@Deprecated
	public static final String USER_HASH_PROP = "hash";

	/**
	 * The key used by the 'new' update process. (We really must find a better
	 * name than 'new'...) Also, we do not reuse the old key.
	 */
	public static final String USER_HASH_PROP_2 = "user.key";

	public static final String EMAIL_PROP = "email";

	public static final String PROFILES_UPDATED = "profiles.updated";

	public static final String MOSYNC_HOME_UPDATED = "mosync.home.updated";

	public static final int LEGACY_PROFILE_TYPE = 1;

	public static final int DEFAULT_PROFILE_TYPE = 0;

	public static final int BINARY_VERSION = 0;

	public static final int BUILD_DATE = 1;

	public static final int MOSYNC_GIT_HASH = 2;

	public static final int ECLIPSE_GIT_HASH = 3;

	private static MoSyncTool instance = new MoSyncTool(true);

	private boolean inited = false;

	private Map<String, String> featureDescriptions = new TreeMap<String, String>();

	/**
	 * This is a reverse map: feature descriptions -> feature ids. (This is
	 * because of 'old' project format storing the descriptions instead of the
	 * id's.
	 */
	private final Map<String, String> featureDescriptionToIdMap = new TreeMap<String, String>();

	private TreeMap<String, String> properties = new TreeMap<String, String>();

	private final PropertyChangeSupport listeners = new PropertyChangeSupport(
			this);

	private IPath overrideHome;

	private LegacyProfileManager legacyProfileManager;

	private ProfileDBManager defaultProfileManager;

	private int profileManagerType;

	private String[] versionInfo;

	private MoSyncTool(boolean initListeners) {
		if (initListeners) {
			addPreferenceStoreListeners();
		}
	}

	private static MoSyncTool createMoSyncTool(IPath overrideHome) {
		MoSyncTool tool = new MoSyncTool(false);
		tool.overrideHome = overrideHome;
		return tool;
	}

	private void addPreferenceStoreListeners() {
		if (CoreMoSyncPlugin.getDefault() != null) {
			CoreMoSyncPlugin.getDefault().getPreferenceStore()
					.addPropertyChangeListener(new IPropertyChangeListener() {
						@Override
						public void propertyChange(
								org.eclipse.jface.util.PropertyChangeEvent event) {
							reinit();
							listeners
									.firePropertyChange(new PropertyChangeEvent(
											this, MOSYNC_HOME_UPDATED, event
													.getOldValue(), event
													.getNewValue()));
						}
					});
		}
	}

	public static MoSyncTool getDefault() {
		return instance;
	}

	public IPath getMoSyncHome() {
		if (overrideHome != null) {
			return overrideHome;
		} else {
			return getMoSyncHomeFromEnv();
		}
	}

	/**
	 * Determines whether a specified home directory constitutes a proper mosync
	 * home directory
	 *
	 * @param home
	 * @return
	 */
	public static boolean isValidHome(IPath home) {
		MoSyncTool guess = createMoSyncTool(home);
		return guess.isValid();
	}

	/**
	 * Returns the default home directory as described in the system environment
	 * variable <code>MOSYNCDIR</code>.
	 *
	 * @return An empty path if no <code>MOSYNCDIR</code> environment variable
	 *         is set.
	 */
	public static IPath getMoSyncHomeFromEnv() {
		String env = System.getenv(MOSYNC_ENV_VAR);
		if (env != null) {
			return new Path(env);
		}

		return new Path("");
	}

	/**
	 * Returns the <code>bin</code> directory, where all binaries are located.
	 *
	 * @return
	 */
	public IPath getMoSyncBin() {
		return getMoSyncHome().append("bin");
	}

	/**
	 * Returns the <code>lib</code> directory, where all binary libs are
	 * located.
	 *
	 * @return
	 */
	public IPath getMoSyncLib() {
		return getMoSyncHome().append("lib");
	}
	
	/**
	 * Returns the extension directory
	 */
	public IPath getMoSyncExtensions() {
		return getMoSyncHome().append("extensions");
	}

	/**
	 * Returns the <code>examples</code> directory, where all MoSync example
	 * projects are stored.
	 *
	 * @return
	 */
	public IPath getMoSyncExamplesDirectory() {
		return getMoSyncHome().append("examples");
	}

	/**
	 * Returns the directory of the example workspace.
	 *
	 * @return
	 */
	public IPath getMoSyncExamplesWorkspace() {
		return getMoSyncExamplesDirectory().append("workspace");
	}

	public IPath[] getMoSyncDefaultIncludes() {
		return new IPath[] { getMoSyncHome().append("include") };
	}

	public IPath[] getMoSyncDefaultLibraryPaths() {
		return new IPath[] { getMoSyncLib().append("pipe") };
	}

	public IPath getProfilesPath() {
		return getMoSyncHome().append("profiles");
	}

	public ProfileManager getProfileManager(int type) {
		switch (type) {
		case DEFAULT_PROFILE_TYPE:
			return defaultProfileManager();
		case LEGACY_PROFILE_TYPE:
			return legacyProfileManager();
		default:
			throw new IllegalArgumentException();
		}
	}

	private synchronized LegacyProfileManager legacyProfileManager() {
		if (legacyProfileManager == null) {
			legacyProfileManager = new LegacyProfileManager();
			legacyProfileManager.init();
		}
		return legacyProfileManager;
	}

	private synchronized ProfileDBManager defaultProfileManager() {
		if (defaultProfileManager == null) {
			defaultProfileManager = ProfileDBManager.getInstance();
			defaultProfileManager.init();
		}
		return defaultProfileManager;
	}

	private IVendor[] getVendors() {
		return getProfileManager(profileManagerType).getVendors();
	}

	public void reinit() {
		if (inited) {
			saveProperties();
		}
		inited = false;
		init();
	}

	private synchronized void init() {
		if (!inited) {
			try {
				if (!isValid()) {
					return;
				}
				initFeatureDescriptions();
				initProperties();
				profileManagerType = ProfileDBManager.isAvailable() ? DEFAULT_PROFILE_TYPE
						: LEGACY_PROFILE_TYPE;
			} finally {
				inited = true;
				listeners.firePropertyChange(PROFILES_UPDATED, null, this);
			}
		}
	}

	// Inits the properties of this tool, using the existing property structure
	private void initProperties() {
		try {
			properties = new TreeMap<String, String>();

			Document doc = getPropertiesXMLDoc();
			NodeList propertyChildren = doc.getElementsByTagName("Property");
			for (int i = 0; i < propertyChildren.getLength(); i++) {
				Node child = propertyChildren.item(i);
				if (child instanceof Element) {
					Element element = (Element) child;
					String name = element.getAttribute("name");
					String value = element.getAttribute("value");
					if (name != null && value != null) {
						properties.put(name, value);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			// Add to some state var!
		}
	}

	private void saveProperties() {
		if (!isValid()) {
			return;
		}

		OutputStream output = null;
		try {
			Document doc = getPropertiesXMLDoc();

			NodeList configNode = doc.getElementsByTagName("Config");
			Element configElement = null;
			if (configNode.getLength() > 0) {
				configElement = (Element) configNode.item(0);
			} else {
				configElement = doc.createElement("Config");
			}

			NodeList children = configElement.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child instanceof Element) {
					Element element = (Element) child;
					if (element.getTagName().equals("Property")) {
						configElement.removeChild(child);
					}
				}
			}

			for (Map.Entry<String, String> property : properties.entrySet()) {
				Element propertyElement = (Element) configElement
						.appendChild(doc.createElement("Property"));
				propertyElement.setAttribute("name", property.getKey());
				propertyElement.setAttribute("value", property.getValue());
			}

			output = new FileOutputStream(getPropertiesFile().toFile());
			OutputFormat format = new OutputFormat();
			format.setLineSeparator("\n");
			format.setIndent(2);
			format.setLineWidth(132);
			XMLSerializer serializer = new XMLSerializer(output, format);
			serializer.asDOMSerializer();
			serializer.serialize(doc.getDocumentElement());
		} catch (Exception e) {
			e.printStackTrace();
			// Add to some state var!
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					// Ignore.
				}
			}
		}
	}

	private Document getPropertiesXMLDoc() throws ParserConfigurationException,
			IOException {
		File propertiesFile = getPropertiesFile().toFile();
		if (!propertiesFile.getParentFile().exists()) {
			propertiesFile.getParentFile().mkdirs();
		}

		if (propertiesFile.exists()) {
			try {
				return DocumentBuilderFactory.newInstance()
						.newDocumentBuilder().parse(propertiesFile);
			} catch (SAXException e) {
				// Just ignore - we'll create a doc from scratch.
			}
		}

		Document doc = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder().newDocument();
		Element root = doc.createElement("Config");
		doc.appendChild(root);

		return doc;
	}

	private IPath getPropertiesFile() {
		return getMoSyncHome().append("etc").append("config.xml");
	}

	public IPath getVendorsPath() {
		IPath profiles = getProfilesPath();
		return profiles.append("vendors");
	}

	public IPath getVendorPath(IVendor vendor) {
		return getVendorsPath().append(vendor.getName());
	}

	public IPath getProfilePath(IProfile profile) {
		return getVendorPath(profile.getVendor()).append(profile.getName());
	}

	public IPath getProfileInfoFile(IProfile profile) {
		return getProfilePath(profile).append("maprofile.h");
	}

	public IPath getTemplatesPath() {
		return getMoSyncHome().append("templates");
	}

	private void initFeatureDescriptions() {
		if (!isValid()) {
			return;
		}

		IPath featureDescriptionsPath = getProfilesPath().append("vendors")
				.append("definitions.txt");
		try {
			featureDescriptions = new ProfileParser()
					.parseFeatureDescriptionFile(featureDescriptionsPath
							.toFile());
		} catch (IOException e) {
			CoreMoSyncPlugin
					.getDefault()
					.getLog()
					.log(new Status(
							IStatus.ERROR,
							CoreMoSyncPlugin.PLUGIN_ID,
							"Could not parse feature description file - this may affect device filtering",
							e));
		}

		for (Iterator<Map.Entry<String, String>> featureDescriptionIterator = featureDescriptions
				.entrySet().iterator(); featureDescriptionIterator.hasNext();) {
			Map.Entry<String, String> entry = featureDescriptionIterator.next();
			featureDescriptionToIdMap.put(entry.getValue(), entry.getKey());
		}
	}

	public String getFeatureDescription(String featureId) {
		init();
		return featureDescriptions.get(featureId);
	}

	public String getFeatureId(String featureDescription) {
		init();
		return featureDescriptionToIdMap.get(featureDescription);
	}

	public String[] getAvailableFeatureDescriptions() {
		init();
		return featureDescriptions.keySet().toArray(new String[0]);
	}

	public String[] getAvailableFeatureDescriptions(Filter<String> filter) {
		init();
		return Filter.filterMap(featureDescriptions, filter).keySet()
				.toArray(new String[0]);
	}

	/**
	 * Returns the default emulator profile.
	 *
	 * @return
	 */
	public IProfile getDefaultEmulatorProfile() {
		init();
		IVendor defaultVendor = legacyProfileManager.getVendor("MoSync");
		if (defaultVendor != null) {
			return defaultVendor.getProfile("Emulator");
		}

		return null;
	}

	public IPath getRuntimePath(IProfile targetProfile) {
		return getRuntimePath(targetProfile.getRuntime());
	}

	public IPath getRuntimePath(String platform) {
		init();
		// The platform is always a /-separated path
		return getMoSyncHome().append(platform);
	}

	public String getProperty(String key) {
		init();
		return properties.get(key);
	}

	public void setProperty(String key, String value) {
		init();
		if (value == null) {
			properties.remove(key);
		} else {
			properties.put(key, value);
		}
		saveProperties();
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		listeners.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		listeners.removePropertyChangeListener(listener);
	}

	/**
	 * Validate this tool, and returns a proper error message if it's not
	 * configured properly.
	 *
	 * @return
	 */
	public String validate() {
		if (getMoSyncHome() == null || getMoSyncHome().isEmpty()) {
			return "the MOSYNCDIR environment variable is not set properly";
		} else if (!getMoSyncBin().toFile().exists()) {
			return "Invalid MoSync home - could not find bin directory";
		} else if (!getProfilesPath().toFile().exists()) {
			return "Invalid MoSync home - could not find profiles directory";
		} else if (!getVendorsPath().toFile().exists()) {
			return "Invalid MoSync home - could not find vendors directory";
		} else if (inited && getProfileManager(DEFAULT_PROFILE_TYPE).getDefaultTargetProfile() == null) {
			// Please note that we need the inited check to avoid an infinite
			// loop.
			return "Tool in an incorrect state - no default target profile exists (so there seems to be something seriously strange with the directory structure of mosync - did you try to trick the IDE?)";
		}

		return null;
	}

	public boolean isValid() {
		return validate() == null;
	}

	/**
	 * Returns a path to the binary with the given name. If no such binary can
	 * be found, null will be returned.
	 *
	 * @param name
	 *            Name of the binary, it should not contain the platform
	 *            specific extension, e.g. ".exe".
	 * @return Path to the binary
	 */
	public IPath getBinary(String name) {

		String extension = getBinExtension();
		return getMoSyncBin().append(name + extension);
	}

	/**
	 * Returns the path to java binary defined by the java.home property.
	 *
	 * @return the path to java binary defined by the java.home property.
	 */
	public IPath getJava() {
		IPath javaHome = new Path(System.getProperty("java.home"));
		IPath javaBin = javaHome.append("bin/java" + getBinExtension());

		return javaBin;
	}

	/**
	 * Returns the extension of executables on the current platform, including
	 * the period sign (.) if applicable.
	 */
	public static String getBinExtension() {
		String extension = "";

		if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
			extension = ".exe";
		}

		return extension;
	}

	/**
	 * Returns the version (build number) of the current set of MoSync binaries.
	 *
	 * @return
	 */
	public String getVersionInfo(int versionInfoType) {
		initVersionInfo();
		if (versionInfoType < versionInfo.length && versionInfoType >= 0) {
			return versionInfo[versionInfoType];
		}
		return "";
	}

	private void initVersionInfo() {
		if (versionInfo == null) {
			versionInfo = new String[0];
			File versionFile = MoSyncTool.getDefault().getMoSyncBin()
			.append("version.dat").toFile();
			if (versionFile.exists()) {
				try {
					String fullVersionInfo = Util.readFile(versionFile.getAbsolutePath());
					versionInfo = fullVersionInfo.split("\\s*\\r*\\n");
				} catch (IOException e) {
					// Fallback
				}
			}
		}

	}
	private String getCurrentVersionFromFile(File versionFile) {
		if (versionFile.exists()) {
			try {
				return readVersion(versionFile);
			} catch (IOException e) {
				// Fallback
			}
		}
		return UNVERSIONED;
	}

	private String readVersion(File versionFile) throws IOException {
		FileReader input = new FileReader(versionFile);
		try {
			LineNumberReader lineInput = new LineNumberReader(input);
			String version = lineInput.readLine();
			if (version != null) {
				return version;
			}
		} catch (Exception e) {
			// Fall-thru
		} finally {
			Util.safeClose(input);
		}
		return UNVERSIONED;
	}

	/**
	 * Returns the registration key for this MoSync tool installation.
	 *
	 * @return
	 */
	public String getRegistrationKey() {
		// TODO Should be fetched from mosync web site
		String hash = getProperty(USER_HASH_PROP);
		if (hash != null) {
			return hash.substring(0, hash.length() / 2);
		}

		return "unregistered";
	}

	/**
	 * The 'inverse' of getProfile(fullName).
	 *
	 * @param preferredProfile
	 * @return
	 */
	public static String toString(IProfile preferredProfile) {
		if (preferredProfile == null) {
			return "";
		} else {
			return preferredProfile.getVendor() + "/"
					+ preferredProfile.getName();
		}
	}

}
