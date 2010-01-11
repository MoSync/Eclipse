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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;

import com.mobilesorcery.sdk.internal.ParseException;
import com.mobilesorcery.sdk.internal.ProfileInfoParser;
import com.mobilesorcery.sdk.internal.SLD;
import com.mobilesorcery.sdk.internal.SLDInfo;
import com.mobilesorcery.sdk.internal.SLDParser;
import com.mobilesorcery.sdk.internal.dependencies.DependencyManager;
import com.mobilesorcery.sdk.internal.dependencies.LibraryLookup;
import com.mobilesorcery.sdk.profiles.ICompositeDeviceFilter;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.ITargetProfileProvider;
import com.mobilesorcery.sdk.profiles.IVendor;
import com.mobilesorcery.sdk.profiles.filter.AbstractDeviceFilter;
import com.mobilesorcery.sdk.profiles.filter.CompositeDeviceFilter;

/**
 * This is a wrapper to provider mosync-specific capabilities to a vanilla IProject,
 * eg special properties, persistence, etc
 * @author Mattias
 *
 */
public class MoSyncProject implements IPropertyOwner, ITargetProfileProvider {

    private final class DeviceFilterListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent event) {
            updateProjectSpec();
            // Then just pass it along.
            firePropertyChange(event);                
        }
    }

    /**
     * The property change event type that is triggered if the target profile
     * of this project changes
     */
    public static final String TARGET_PROFILE_CHANGED = "target.profile";

    /**
     * The property change event type that is triggered if the build configuration
     * of this project changes
     */
    public static final String BUILD_CONFIGURATION_CHANGED = "build.config";

    /**
     * The property change event type that is triggered if the build configuration
     * support of this project changes
     */
    public static final String BUILD_CONFIGURATION_SUPPORT_CHANGED = "build.config.support";

    /**
     * The file exclude filter property for this project.
     * TODO: Not implemented yet
     * @see PathExclusionFilter
     */
    public static final String EXCLUDE_FILTER_KEY = "excludes";

    /**
     * The project type of MoSync Projects (used only by CDT).
     */
	public static final String C_PROJECT_ID = CoreMoSyncPlugin.PLUGIN_ID + ".project";

	/**
	 * The property initializer context of MoSyncProjects
	 * @see IPropertyInitializer
	 */
	public static final String CONTEXT = "com.mobilesorcery.sdk.mosync.project.context";

	/**
	 * The property key for the originating template id of this project.
	 */
    public static final String TEMPLATE_ID = "template.id";

	/**
	 * The property key for the dependency strategy of this project.
	 */
	public static final String DEPENDENCY_STRATEGY = "dependency.strategy";
	
	/**
	 * A constant indicating that this project should use the GCC dependency
	 * strategy (GCC -MF option).
	 */
	public static final int GCC_DEPENDENCY_STRATEGY = 0;

	/**
	 * A constant indicating that this project should not use any dependency
	 * strategy at all (always rebuild).
	 */
	public static final int NULL_DEPENDENCY_STRATEGY = 1;

    private static final String PROJECT = "project";    

    private static final String TARGET = "target-profile";    

    private static final String VENDOR_KEY = "vendor";

    private static final String PROFILE_KEY = "device";

    private static final String PROPERTIES = "properties";

    private static final String PROPERTY = "property";

    private static final String KEY_KEY = "key";

    private static final String VALUE_KEY = "value";
    
	private static final String BUILD_CONFIG = "build.cfg";

	private static final String BUILD_CONFIG_ID_KEY = "id";

	private static final String BUILD_CONFIGS_SUPPORTED = "supports-build-configs";

	private static final String ACTIVE_BUILD_CONFIG_KEY = "active";

	/**
	 * The name of the file where this project's meta data is located.
	 */
	public static final String MOSYNC_PROJECT_META_DATA_FILENAME = ".mosyncproject";

    private static IdentityHashMap<IProject, MoSyncProject> projects = new IdentityHashMap<IProject, MoSyncProject>();
    
    private IProject project;

    private IProfile target;

    private static PropertyChangeSupport globalListeners = new PropertyChangeSupport(new Object());
    
    private PropertyChangeSupport listeners = new PropertyChangeSupport(this);

    private ICompositeDeviceFilter deviceFilter = new CompositeDeviceFilter(new IDeviceFilter[0]);
    
    private Map<String, String> properties = new HashMap<String, String>();

    private DeviceFilterListener deviceFilterListener;

	private DependencyManager<IResource> dependencyManager = new DependencyManager<IResource>();

	private IBuildConfiguration currentBuildConfig;

	private TreeMap<String, IBuildConfiguration> configurations = new TreeMap<String, IBuildConfiguration>(String.CASE_INSENSITIVE_ORDER);

	private boolean isBuildConfigurationsSupported;

	private HashMap<String, SLD> slds = new HashMap<String, SLD>();

    private MoSyncProject(IProject project) {
        Assert.isNotNull(project);
        this.project = project;
        this.deviceFilterListener = new DeviceFilterListener();
        initFromProjectMetaData(getMoSyncProjectMetaDataLocation());        
        addDeviceFilterListener();
    }

    private void addDeviceFilterListener() {        
        deviceFilter.addPropertyChangeListener(deviceFilterListener);
    }
    
    private void removeDeviceFilterListener() {
        deviceFilter.removePropertyChangeListener(deviceFilterListener);
    }

    private void initFromProjectMetaData(IPath projectMetaDataPath) {
    	if (projectMetaDataPath == null) {
    		projectMetaDataPath = getMoSyncProjectMetaDataLocation();
    	}
    	
        if (!projectMetaDataPath.toFile().exists()) {
            return;
        }
        
        FileReader input = null;
        
        try {
            input = new FileReader(projectMetaDataPath.toFile());
            XMLMemento memento = XMLMemento.createReadRoot(input);
            initTargetProfileFromProjectMetaData(memento);
            initAvailableBuildConfigurations(memento);
            
            deviceFilter = CompositeDeviceFilter.read(memento);
            properties = initPropertiesFromProjectMetaData(memento);
        } catch (Exception e) {
            CoreMoSyncPlugin.getDefault().log(e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    CoreMoSyncPlugin.getDefault().log(e);
                }
            }
        }
        
    }

    private void initAvailableBuildConfigurations(XMLMemento memento) {
    	Boolean supportsBuildConfigurations = memento.getBoolean(BUILD_CONFIGS_SUPPORTED);
    	this.isBuildConfigurationsSupported = supportsBuildConfigurations == null ? false : supportsBuildConfigurations;
    	    	
    	configurations.clear();
    	IMemento[] cfgs = memento.getChildren(BUILD_CONFIG);
    	for (int i = 0; i < cfgs.length; i++) {
    		String id = cfgs[i].getString(BUILD_CONFIG_ID_KEY);
    		BuildConfiguration cfg = new BuildConfiguration(this, id);
    		configurations.put(id, cfg);
    		if (Boolean.TRUE.equals(cfgs[i].getBoolean(ACTIVE_BUILD_CONFIG_KEY))) {
    			currentBuildConfig = cfg;
    		}
    	}
	}

	private Map<String, String> initPropertiesFromProjectMetaData(XMLMemento memento) {
        Map<String, String> result = new HashMap<String, String>();
        IMemento properties = memento.getChild(PROPERTIES);
        if (properties != null) {
            IMemento[] propertyChildren = properties.getChildren(PROPERTY);
            for (int i = 0; propertyChildren != null && i < propertyChildren.length; i++) {
                String key = propertyChildren[i].getString(KEY_KEY);
                String value = propertyChildren[i].getString(VALUE_KEY);
                if (key != null && value != null) {
                    result.put(key, value);
                }
            }
        }
        
        return result;
    }

    private void initTargetProfileFromProjectMetaData(IMemento memento) {
        IMemento targetMemento = memento.getChild(TARGET);
        if (targetMemento != null) {
            String vendorName = targetMemento.getString(VENDOR_KEY);
            String profileName = targetMemento.getString(PROFILE_KEY);
            IVendor vendor = vendorName == null ? null : MoSyncTool.getDefault().getVendor(vendorName);
            if (vendor != null) {
                IProfile targetProfile = vendor.getProfile(profileName);
                if (targetProfile != null) {
                    target = targetProfile;
                }
            }
        }
    }

    protected void updateProjectSpec() {
        IPath projectMetaDataPath = getMoSyncProjectMetaDataLocation();
        XMLMemento root = XMLMemento.createWriteRoot(PROJECT);
        FileWriter output = null;

        try {
            output = new FileWriter(projectMetaDataPath.toFile());
            IMemento target = root.createChild(TARGET);
            saveTargetProfile(target);
            saveAvailableBuildConfigurations(root);
            
            deviceFilter.saveState(root);
            
            IMemento properties = root.createChild(PROPERTIES);
            saveProperties(properties);
            
            root.save(output);
            output.close();
        } catch (IOException e) {
            CoreMoSyncPlugin.getDefault().log(e);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    CoreMoSyncPlugin.getDefault().log(e);
                }
            }
        }
    }
    
    private void saveAvailableBuildConfigurations(XMLMemento root) {
    	// Older versions of the project file format do not have build configs,
    	// so we save a marker denoting that we do.
    	root.putBoolean(BUILD_CONFIGS_SUPPORTED, isBuildConfigurationsSupported);
    	for (String cfg : getBuildConfigurations()) {
    		IMemento node = root.createChild(BUILD_CONFIG);
    		node.putString(BUILD_CONFIG_ID_KEY, cfg);
    		if (currentBuildConfig != null && cfg.equals(currentBuildConfig.getId())) {
    			node.putBoolean(ACTIVE_BUILD_CONFIG_KEY, true);
    		}
    	}    	
	}

	private void saveProperties(IMemento memento) {
		TreeMap<String, String> sortedProperties = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		sortedProperties.putAll(this.properties);
        for (Iterator<String> properties = sortedProperties.keySet().iterator(); properties.hasNext(); ) {
            String key = properties.next();
            String value = this.properties.get(key);
            IMemento property = memento.createChild(PROPERTY);
            property.putString(KEY_KEY, key);
            property.putString(VALUE_KEY, value);
        }
    }

    private void saveTargetProfile(IMemento memento) {
        if (target != null) {
            memento.putString(VENDOR_KEY, target.getVendor().getName());
            memento.putString(PROFILE_KEY, target.getName());
        }
    }

    public IPath getMoSyncProjectMetaDataLocation() {
        return project.getLocation().append(MOSYNC_PROJECT_META_DATA_FILENAME);
    }

    /**
     * <p>Returns a (shared) instance of a MoSyncProject of
     * the provided project, or <code>null</code> if
     * the project does not have a MoSync nature.</p>
     * @param project The eclipse project this <code>MoSyncProject</code> should wrap
     * @return
     */
    public synchronized static MoSyncProject create(IProject project) {
        try {
            if (!MoSyncNature.isCompatible(project)) {
                return null;
            }
        } catch (CoreException e) {
            return null;
        }
        
        MoSyncProject result = projects.get(project);
        if (result == null) {
            result = new MoSyncProject(project);
            projects.put(project, result);
        }

        return result;
    }
    
    /**
     * <p>Returns a (shared) instance of a MoSyncProject of
     * the provided project, or <code>null</code> if
     * the project does not have a MoSync nature.</p>
     * <p>An optional project meta data file initialization file may
     * be provided. This file will then be loaded and its project meta
     * data used by the project.</p>
     * @param project The eclipse project this <code>MoSyncProject</code> should wrap
     * @param projectMetadataLocation The location of the project meta data. If <code>null</code>,
     * no initialization will take place unless this is the first call to <code>create</code> 
     * with the provided <code>project</code>.
     * @return
     */
    public static MoSyncProject create(IProject project, IPath projectMetadataLocation) {
    	MoSyncProject result = create(project);
        if (projectMetadataLocation != null) {
        	result.initFromProjectMetaData(projectMetadataLocation);
        }
        
        return result;
    }
    
    /**
     * <p>Returns the current target profile of this MoSync project.</p>
     * <p>If none is set, a default target profile is returned.</p>
     */
    public IProfile getTargetProfile() {
        return target == null ? MoSyncTool.getDefault().getDefaultTargetProfile() : target;
    }
    
    /**
     * <p>Sets the current target profile of this MoSync project, and
     * notifies all listeners about this change.</p>
     * @param newTarget The new target profile
     */
    public void setTargetProfile(IProfile newTarget) {
    	IProfile oldTarget = initTargetProfile(newTarget);
        firePropertyChange(new PropertyChangeEvent(this, TARGET_PROFILE_CHANGED, oldTarget, newTarget));
    }
    
    /**
     * Initializes the target profile; same as <code>setTargetProfile</code>,
     * but no event is fired.
     * @param newTarget
     * @return The old target profile.
     */
    public IProfile initTargetProfile(IProfile newTarget) {
        IProfile oldTarget = this.target;
        this.target = newTarget;
        updateProjectSpec();
        return oldTarget;
    }
    
    private void firePropertyChange(PropertyChangeEvent event) {
    	globalListeners.firePropertyChange(event);
        listeners.firePropertyChange(event);
	}

	/**
	 * <p>Adds a <emph>global</emph> property listener, ie a listener that
	 * listens to changes to <emph>all</emph> MoSync projects in a workspace.</p>
	 * @param globalListener The listener to add
	 */
	public static void addGlobalPropertyChangeListener(PropertyChangeListener globalListener) {
		globalListeners.addPropertyChangeListener(globalListener);		
	}
	
	/**
	 * <p>Removes a <emph>global</emph> property listener, ie a listener that
	 * listens to changes to <emph>all</emph> MoSync projects in a workspace.</p>
	 * @param globalListener The listener to remove
	 */
	public static void removeGlobalPropertyChangeListener(PropertyChangeListener globalListener) {
		globalListeners.removePropertyChangeListener(globalListener);				
	}
	
	/**
	 * <p>Adds a property listener to this project.</p>
	 * <p>Property listeners are notified about events such
	 * as changing target profile or any other project property change
	 * set by <code>setProperty</code>.
	 * @param globalListener The listener to add
	 */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        listeners.addPropertyChangeListener(listener);
    }
    
	/**
	 * <p>Removes a property listener from this project.</p>
	 * @param globalListener The listener to remove
	 */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        listeners.removePropertyChangeListener(listener);
    }

    /**
     * <p>Returns the <code>ICompositeDeviceFilter</code> currently
     * associated with this project.</p>
     * @return
     */
    public ICompositeDeviceFilter getDeviceFilter() {
        return deviceFilter;
    }
    
    /**
     * <p>Sets the <code>ICompositeDeviceFilter</code> currently
     * associated with this project.</p>
     * @param deviceFilter The new filter
     */
    public void setDeviceFilter(ICompositeDeviceFilter deviceFilter) {
        ICompositeDeviceFilter oldFilter = this.deviceFilter;
        removeDeviceFilterListener();
        this.deviceFilter = deviceFilter;
        addDeviceFilterListener();
        listeners.firePropertyChange(new PropertyChangeEvent(this, IDeviceFilter.FILTER_CHANGED, oldFilter, deviceFilter));
    }
    
    /**
     * <p>Returns the path to the STABS debug info file of a specific build configuration.</p>
     * @param buildConfiguration 
     * @return
     */
	public IPath getStabsPath(IBuildConfiguration buildConfiguration) {
		IPath outputPath = MoSyncBuilder.getOutputPath(project, getPropertyOwner(buildConfiguration)).append("stabs.tab");
		return outputPath;
	}

	/**
	 * Returns the SLD for a specific buildconfiguration; if a null
	 * build configuration is passed as argument, then this amounts
	 * to build configurations not being supported.
	 * @param buildConfiguration
	 * @return
	 */
	public synchronized SLD getSLD(IBuildConfiguration buildConfiguration) {
		IPath outputPath = MoSyncBuilder.getOutputPath(project, getPropertyOwner(buildConfiguration)).append("Sld.tab");
		SLD sld = slds.get(outputPath.toPortableString());
		if (sld == null) {
			sld = new SLD(this, outputPath);
			slds.put(outputPath.toPortableString(), sld);
		}
		
		return sld;
	}
	
    /**
     * Parses the profile info file for a project,
     * and returns 
     * @return
     * @throws IOException 
     * @throws ParseException
     * @deprecated Well, we're not using it right now; when we do,
     * lets un-deprecate this 
     */
    public IProfileInfo parsePerformanceInfo(IPath profileInfoFile, IProgressMonitor monitor) throws ParseException, IOException {
    	throw new UnsupportedOperationException();
    	/*monitor.beginTask("Parsing profile info", 2);
    	SLD sld = parseSLD();
    	monitor.worked(1);
    	IProfileInfo result = ProfileInfoParser.parse(profileInfoFile, sld, monitor);
    	monitor.done();
    	return result;*/
    }
    
    /**
     * Returns the underlying Eclipse project of this MoSync project.
     * @return
     */
    public IProject getWrappedProject() {
        return project;
    }

    /**
     * <p>Returns a project-specific property,
     * using default properties set by property
     * initializers if necessary.</p>
     */
    public String getProperty(String key) {
        String property = properties.get(key);
        if (property == null) {
        	return getDefaultProperty(key);
        }
        
        return property;
    }
    
    public Map<String, String> getProperties() {
    	return new TreeMap<String, String>(properties);
    }

    /**
     * <p>Sets a project-specific property.</p>
     * @param key 
     * @param value
     * @return <code>true</code> if and only if the property was changed
     */
    public boolean setProperty(String key, String value) {
    	String oldValue = getProperty(key);
    	if (NameSpacePropertyOwner.equals(oldValue, value)) {
    		return false;
    	}
    	
    	System.err.println(key + " := " + value);
    	
    	initProperty(key, value);

        updateProjectSpec();
        firePropertyChange(new PropertyChangeEvent(this, key, oldValue, value));
        
        return true;
    }
    
	public boolean applyProperties(Map<String, String> properties) {
		boolean result = false;
		for (String key : properties.keySet()) {
			String value = properties.get(key);
			result |= setProperty(key, value);
		}
		
		return result;
	}

	public IWorkingCopy createWorkingCopy() {
		return new PropertyOwnerWorkingCopy(this);
	}

    /**
     * <p>Returns the list of vendors to use for this project,
     * using the device filter for this project.</p>
     * <p>To get a list of all vendors, use MoSyncTool.getVendors.</p>
     * @return
     */
	public IVendor[] getFilteredVendors() {
        IVendor[] allVendors = MoSyncTool.getDefault().getVendors();
        return AbstractDeviceFilter.filterVendors(allVendors, deviceFilter);
    }    

    /**
     * <p>Returns the list of profiles to use for this project,
     * using the device filter associated with this project.</p>
     * <p>To get a list of all profiles, use MoSyncTool.getProfiles.</p>
     * @return
     */
	public IProfile[] getFilteredProfiles() {
		IProfile[] profiles = MoSyncTool.getDefault().getProfiles();
		ArrayList<IProfile> filtered = new ArrayList<IProfile>();
		for (int i = 0; i < profiles.length; i++) {
			if (deviceFilter.accept(profiles[i])) {
				filtered.add(profiles[i]);
			}
		}
		
		return filtered.toArray(new IProfile[filtered.size()]);
	}

	/**
	 * Returns the default value of a project specific property
	 */
	public String getDefaultProperty(String key) {
		return CoreMoSyncPlugin.getDefault().getDefaultValue(this, key);
	}
	
	/**
	 * <p>Sets the property, regardless of any previous
	 * value.</p> 
	 * @param key
	 * @param value If <code>null</code>, then the entry is removed  
	 */
	public void initProperty(String key, String value) {
		if (value == null) {
			properties.remove(key);
		} else {
			properties.put(key, value);
		}
	}
	
	/**
	 * A convenience method for returning the exclusion filter for a project/config.
	 * @param properties
	 * @return
	 */
	public static PathExclusionFilter getExclusionFilter(MoSyncProject project) {
		if (project == null) {
			return null;
		}
		// Efficient?
		return PathExclusionFilter.parse(PropertyUtil.getStrings(project.getPropertyOwner(), EXCLUDE_FILTER_KEY));
	}
	
	public static void setExclusionFilter(MoSyncProject project, PathExclusionFilter filter) {
		PropertyUtil.setStrings(project.getPropertyOwner(), EXCLUDE_FILTER_KEY, filter.getFileSpecs());
	}
	
	public String getContext() {
		return CONTEXT;
	}

	/**
	 * Returns the name of this MoSync project.
	 * @return
	 */
    public String getName() {
        return getWrappedProject().getName();
    }

    /**
     * Adds a MoSync Project Nature to an Eclipse project.
     * @param project
     */
    public static void addNatureToProject(IProject project) {
        MoSyncNature.addNatureToProject(project);
    }

    /**
     * Adds all properties of <code>properties</code> to this project.
     * @param properties
     */
    public void setProperties(Map<String, String> properties) {
        this.properties.putAll(properties);
        updateProjectSpec();
    }

    /**
     * Returns the dependency manager of this project.
     * @return
     */
	public DependencyManager<IResource> getDependencyManager() {
		return dependencyManager;
	}

	/**
	 * Returns the current build configuration. Does not always return
	 * a build configuration, but will return one regardless of
	 * whatever isBuildConfigurationsSupported returns.
	 * @return
	 */
	public IBuildConfiguration getActiveBuildConfiguration() {
		return currentBuildConfig;
	}

	/**
	 * Sets the current build configuration. A property change
	 * event with event type <code>BUILD_CONFIGURATION_CHANGED</code>
	 * is triggered.
	 * @param id The new build configuration to use
	 * @throws IllegalArgumentException If no build configuration with
	 * the given id exists.
	 */
	public void setActiveBuildConfiguration(String id) {
		IBuildConfiguration oldCfg = getActiveBuildConfiguration();
		Object oldId = oldCfg == null ? null : oldCfg.getId();
		IBuildConfiguration newConfiguration = getBuildConfiguration(id);
		if (newConfiguration == null) {
			throw new IllegalArgumentException(MessageFormat.format("No configuration with id {0}", id));
		}
		currentBuildConfig = newConfiguration;
		updateProjectSpec();
		firePropertyChange(new PropertyChangeEvent(this, BUILD_CONFIGURATION_CHANGED, oldId, id));
	}
	
	public Set<String> getBuildConfigurations() {
		return new TreeSet<String>(configurations.keySet());
	}
	
	public IBuildConfiguration getBuildConfiguration(String id) {
		return configurations.get(id);
	}
	
	public IBuildConfiguration installBuildConfiguration(String id) {
		BuildConfiguration newConfig = new BuildConfiguration(this, id);
		installBuildConfiguration(newConfig);
		return newConfig;
	}
	
	public void installBuildConfiguration(IBuildConfiguration newConfig) {
		String id = newConfig.getId();
		this.configurations.put(id, newConfig);
		updateProjectSpec();
		listeners.firePropertyChange(BUILD_CONFIGURATION_CHANGED, null, newConfig);
	}
	
	public void deinstallBuildConfiguration(String id) {
		IBuildConfiguration removed = this.configurations.remove(id);
		removed.getProperties().clear();
		if (currentBuildConfig == removed) {
			currentBuildConfig = null;
			if (!configurations.isEmpty()) {
				Entry<String, IBuildConfiguration> entry = configurations.lowerEntry(id);
				currentBuildConfig = entry == null ? configurations.firstEntry().getValue() : entry.getValue();
			}
		}
		updateProjectSpec();
		listeners.firePropertyChange(BUILD_CONFIGURATION_CHANGED, removed, null);
	}

	public void setBuildConfigurationsSupported(boolean isBuildConfigurationsSupported) {
		if (this.isBuildConfigurationsSupported != isBuildConfigurationsSupported) {
			this.isBuildConfigurationsSupported = isBuildConfigurationsSupported;
			updateProjectSpec();
			listeners.firePropertyChange(BUILD_CONFIGURATION_SUPPORT_CHANGED, !isBuildConfigurationsSupported, isBuildConfigurationsSupported);
		}
	}
	
	public boolean isBuildConfigurationsSupported() {
		return isBuildConfigurationsSupported;
	}
	
	public void activateBuildConfigurations() {
		setBuildConfigurationsSupported(true);
		
		if (configurations.isEmpty()) {
			configurations.put("Release", new BuildConfiguration(this, IBuildConfiguration.RELEASE_ID));
			configurations.put("Debug", new BuildConfiguration(this, IBuildConfiguration.DEBUG_ID));
			//configurations.put("Test", new BuildConfiguration(this, IBuildConfiguration.TEST_ID));
			setActiveBuildConfiguration("Release");
		}
	}

	public IPropertyOwner getPropertyOwner() {
		if (isBuildConfigurationsSupported && currentBuildConfig != null) {
			return currentBuildConfig.getProperties();
		}
		
		return this;
	}
	
	private IPropertyOwner getPropertyOwner(IBuildConfiguration config) {
		return config == null ? this : config.getProperties();
	}

	public LibraryLookup getLibraryLookup(IPropertyOwner buildProperties) {
		// TODO: cache?
		return new LibraryLookup(MoSyncBuilder.getLibraryPaths(getWrappedProject(), buildProperties), MoSyncBuilder.getLibraries(buildProperties));
	}


}
