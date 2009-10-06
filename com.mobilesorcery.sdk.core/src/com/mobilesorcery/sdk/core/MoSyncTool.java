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
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.IVendor;
import com.mobilesorcery.sdk.profiles.ProfileParser;
import com.mobilesorcery.sdk.profiles.Vendor;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class MoSyncTool {

    public final static String CONSTANT_PREFIX = "MA_PROF_CONST_";

    /**
     * A filter for excluding constants from the constants.
     */
    public final static Filter<String> INCLUDE_CONSTANTS_FILTER = new Filter<String>() {
        public boolean accept(String obj) {
            return obj != null && obj.startsWith(CONSTANT_PREFIX);
        }
    };

    public final static String MOSYNC_HOME_PREF = "mosync.home";
    
    public static final String MO_SYNC_HOME_FROM_ENV_PREF = "mosync.home.env";

	public static final String AUTO_UPDATE_PREF = "mosync.auto.update";
	
    public static final Filter<String> EXCLUDE_CONSTANTS_FILTER = INCLUDE_CONSTANTS_FILTER.inverse();

    private static final String MOSYNC_ENV_VAR = "MOSYNCDIR";

    public static final String USER_HASH_PROP = "hash";

    public static final String EMAIL_PROP = "email";

    public static final String PROFILES_UPDATED = "profiles.updated";

    public static final String MOSYNC_HOME_UPDATED = "mosync.home.updated";

    private static MoSyncTool instance = new MoSyncTool(true);

    private boolean inited = false;

    private TreeMap<String, IVendor> vendors = new TreeMap<String, IVendor>(String.CASE_INSENSITIVE_ORDER);

    private Map<String, String> featureDescriptions = new TreeMap<String, String>();

    /**
     * This is a reverse map: feature descriptions -> feature ids.
     * (This is because of 'old' project format storing the descriptions instead of
     * the id's.
     */
    private Map<String, String> featureDescriptionToIdMap = new TreeMap<String, String>();

    private TreeMap<String, String> properties = new TreeMap<String, String>();

    private PropertyChangeSupport listeners = new PropertyChangeSupport(this);

    private IPath overrideHome;

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
            CoreMoSyncPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {
                public void propertyChange(org.eclipse.jface.util.PropertyChangeEvent event) {
                    reinit();
                    listeners.firePropertyChange(new PropertyChangeEvent(this, MOSYNC_HOME_UPDATED, event.getOldValue(), event
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
        }
        
        if (CoreMoSyncPlugin.getDefault() == null) {
            return getMoSyncHomeFromEnv();
        }

        boolean useEnv = CoreMoSyncPlugin.getDefault().getPreferenceStore().getBoolean(MO_SYNC_HOME_FROM_ENV_PREF);
        if (!useEnv) {
            String home = CoreMoSyncPlugin.getDefault().getPreferenceStore().getString(MOSYNC_HOME_PREF);
            return new Path(home);
        } else {
            return getMoSyncHomeFromEnv();
        }
    }

    /**
     * Tries to guess where to find the MoSync installation - this will be used as
     * a basis for its corresponding preference store default value.
     * @return
     */
    public static IPath guessHome() {
        try {
            IPath installLocation = new Path(Platform.getInstallLocation().getURL().getPath());
            // We guess that the tool is located in a sibling directory of the eclipse-installation directory.
            MoSyncTool guess = createMoSyncTool(installLocation.removeLastSegments(1).append("MoSync"));
            if (guess.isValid()) {
                return guess.getMoSyncHome();
            }
        } catch (Exception e) {
            // Silently ignore, we're just guessing anyway
        }
        
        // By default, we return the default install directory
        return new Path("C:\\MoSync");
    }

    public static boolean isValidHome(IPath home) {
        MoSyncTool guess = createMoSyncTool(home);
        return guess.isValid();
    }
    
    public static IPath getMoSyncHomeFromEnv() {
        String env = System.getenv(MOSYNC_ENV_VAR);
        if (env != null) {
            return new Path(env);
        }

        return null;
    }

    public IPath getMoSyncBin() {
        return getMoSyncHome().append("bin");
    }

    public IPath getMoSyncLib() {
        return getMoSyncHome().append("lib");
    }

    public IPath[] getMoSyncDefaultIncludes() {
        return new IPath[] { getMoSyncHome().append("include") };
    }

    public IPath[] getMoSyncDefaultLibraryPaths() {
        return new IPath[] { getMoSyncLib().append("pipe") };
    }

    public IPath[] getMoSyncDefaultLibraries() {
        return new IPath[] { new Path("MAStd.lib") };
    }

    public IPath getProfilesPath() {
        return getMoSyncHome().append("profiles");
    }

    public IVendor[] getVendors() {
        init();
        return vendors.values().toArray(new IVendor[0]);
    }
    
    public IVendor[] getVendors(IDeviceFilter filter) {
    	IVendor[] allVendors = getVendors();
    	ArrayList<IVendor> result = new ArrayList<IVendor>();
    	
		for (int i = 0; i < allVendors.length; i++) {
    		if (filter.accept(allVendors[i])) {
    			result.add(allVendors[i]);
    		}
    	}
    	
    	return result.toArray(new IVendor[0]);
    }

    public IProfile[] getProfiles() {
        ArrayList<IProfile> profiles = new ArrayList<IProfile>();
        IVendor[] vendors = getVendors();
        for (int i = 0; i < vendors.length; i++) {
            profiles.addAll(Arrays.asList(vendors[i].getProfiles()));
        }

        return profiles.toArray(new IProfile[0]);
    }
    
    public IProfile[] getProfiles(String profileName) {
        ArrayList<IProfile> profiles = new ArrayList<IProfile>();
        IVendor[] vendors = getVendors();
        for (int i = 0; i < vendors.length; i++) {
            IProfile profile = vendors[i].getProfile(profileName);
            if (profile != null) {
                profiles.add(profile);
            }
        }
        
        return profiles.toArray(new IProfile[0]);
    }
    
    public void reinit() {
        saveProperties();
        inited = false;
        init();
    }

    private synchronized void init() {
        if (!inited) {
            try {
                if (!isValid()) {
                    return;
                }

                IPath vendorsPath = getVendorsPath();

                File[] directories = vendorsPath.toFile().listFiles();
                for (int i = 0; i < directories.length; i++) {
                    if (directories[i].isDirectory()) {
                        IVendor vendor = initVendor(directories[i]);
                        this.vendors.put(vendor.getName(), vendor);
                    }
                }

                initFeatureDescriptions();
                initProperties();
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
                Element propertyElement = (Element) configElement.appendChild(doc.createElement("Property"));
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

    private Document getPropertiesXMLDoc() throws ParserConfigurationException, IOException {
        File propertiesFile = getPropertiesFile().toFile();
        if (!propertiesFile.getParentFile().exists()) {
            propertiesFile.getParentFile().mkdirs();
        }

        if (propertiesFile.exists()) {
            try {
                return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(propertiesFile);
            } catch (SAXException e) {
                // Just ignore - we'll create a doc from scratch.
            }
        }

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = doc.createElement("Config");
        doc.appendChild(root);

        return doc;
    }

    private IPath getPropertiesFile() {
        return getMoSyncHome().append("etc").append("config.xml");
    }

    private IVendor initVendor(File vendorDir) {
        ProfileParser parser = new ProfileParser();

        String name = vendorDir.getName();
        File iconFile = new File(vendorDir, "icon.png");
        ImageDescriptor icon = null;
        try {
            if (iconFile.exists()) {
                icon = ImageDescriptor.createFromURL(iconFile.toURI().toURL());
            }
        } catch (MalformedURLException e) {
            // Just ignore.
        }

        Vendor vendor = new Vendor(name, icon);

        File[] profiles = vendorDir.listFiles();
        for (int i = 0; i < profiles.length; i++) {
            if (profiles[i].isDirectory()) {
                String profileName = profiles[i].getName();
                File profileInfoFile = new File(profiles[i], "maprofile.h");
                File runtimeTxtFile = new File(profiles[i], "runtime.txt");
                IProfile profile;
                try {
                    profile = parser.parseInfoFile(vendor, profileName, profileInfoFile, runtimeTxtFile);
                    vendor.addProfile(profile);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        return vendor;
    }

    public IVendor getVendor(String vendorName) {
        init();
        return vendors.get(vendorName);
    }

    /**
     * Given a name of the format <code>vendor/profile</code>, will return an
     * instance of <code>IProfile</code>
     * 
     * @param fullName
     * @return <code>null</code> if <code>fullName</code> is <code>null</code>, or
     * does not properly represent a profile
     */
    public IProfile getProfile(String fullName) {
    	if (fullName == null) {
    		return null;
    	}
    	
        String[] tokens = fullName.split("/");
        if (tokens.length != 2) {
            return null;
        }

        String vendorName = tokens[0];
        String profileName = tokens[1];

        IVendor vendor = getVendor(vendorName);
        if (vendor == null) {
            return null;
        }

        return vendor.getProfile(profileName);
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

        IPath featureDescriptionsPath = getProfilesPath().append("vendors").append("definitions.txt");
        try {
            featureDescriptions = new ProfileParser().parseFeatureDescriptionFile(featureDescriptionsPath.toFile());
        } catch (IOException e) {
            CoreMoSyncPlugin.getDefault().getLog().log(
                    new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID,
                            "Could not parse feature description file - this may affect device filtering", e));
        }
        
        for (Iterator<Map.Entry<String, String>> featureDescriptionIterator = featureDescriptions.entrySet().iterator(); featureDescriptionIterator.hasNext(); ) {
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
        return Filter.filterMap(featureDescriptions, filter).keySet().toArray(new String[0]);
    }

    public IProfile getDefaultTargetProfile() {
    	return getDefaultEmulatorProfile();
    }

    public IProfile getDefaultEmulatorProfile() {
        init();
        IVendor defaultVendor = getVendor("MobileSorcery");
        if (defaultVendor != null) {
            return defaultVendor.getProfile("Emulator");
        }

        return null;
	}

    public IPath getRuntimePath(IProfile targetProfile) {
        init();
        // The platform is always a /-separated path
        String platform = targetProfile.getPlatform();
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
        if (getMoSyncHome() == null) {
            return "MoSync home is not set";
        } else if (!getMoSyncBin().toFile().exists()) {
            return "Invalid MoSync home - could not find bin directory";
        } else if (!getProfilesPath().toFile().exists()) {
            return "Invalid MoSync home - could not find profiles directory";
        } else if (!getVendorsPath().toFile().exists()) {
            return "Invalid MoSync home - could not find vendors directory";
        }

        return null;
    }

    public boolean isValid() {
        return validate() == null;
    }

    /**
     * The 'inverse' of getProfile(fullName).
     * @param preferredProfile
     * @return
     */
	public static String toString(IProfile preferredProfile) {
		if (preferredProfile == null) {
			return "";
		} else {
			return preferredProfile.getVendor() + "/" + preferredProfile.getName();
		}
	}



}
