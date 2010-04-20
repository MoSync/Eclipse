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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;

import com.mobilesorcery.sdk.internal.BuildState;
import com.mobilesorcery.sdk.internal.ParseException;
import com.mobilesorcery.sdk.internal.SLD;
import com.mobilesorcery.sdk.internal.builder.BuildResultManager;
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
     * @see PathExclusionFilter
     */
    public static final String EXCLUDE_FILTER_KEY = "excludes";
    
    /**
     * The standard file excludes for this project (usually NOT an a per-configuration
     * basis).
     */
    public static final String STANDARD_EXCLUDES_FILTER_KEY = "standard.excludes";

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

    private static final String BUILD_CONFIG_TYPES = "types";
	
	private static final String ACTIVE_BUILD_CONFIG = "active.build.cfg";

	private static final String BUILD_CONFIG_ID_KEY = "id";

	private static final String BUILD_CONFIGS_SUPPORTED = "supports-build-configs";

	private static final String ACTIVE_BUILD_CONFIG_KEY = "active";
	
	private static final String VERSION_KEY = "version";

	private static final String VERSION = "1";

	/**
	 * The name of the file where this project's <b>shared</b> meta data is located.
	 */
	public static final String MOSYNC_PROJECT_META_DATA_FILENAME = ".mosyncproject";
	
	/**
	 * The name of the file where this project's <b>local</b> meta data is located.
	 */
	public static final String MOSYNC_LOCAL_PROJECT_META_DATA_FILENAME = ".mosyncproject.local";

	public static final int SHARED_PROPERTY = 0;

	public static final int LOCAL_PROPERTY = 1;


    private static IdentityHashMap<IProject, MoSyncProject> projects = new IdentityHashMap<IProject, MoSyncProject>();
    
    private IProject project;

    private IProfile target;

    private static PropertyChangeSupport globalListeners = new PropertyChangeSupport(new Object());
    
    private PropertyChangeSupport listeners = new PropertyChangeSupport(this);

    private ICompositeDeviceFilter deviceFilter = new CompositeDeviceFilter(new IDeviceFilter[0]);
    
    /**
     * This is the map holding the shared properties of this project, stored in .mosyncproject
     */
    private Map<String, String> sharedProperties = new HashMap<String, String>();
    
    /**
     * This is the map holding the user's local properties of this project, stored in .mosyncproject.local
     */
    private Map<String, String> localProperties = new HashMap<String, String>();
    
    private CascadingProperties properties = new CascadingProperties(new Map[] { sharedProperties, localProperties });

    private DeviceFilterListener deviceFilterListener;

	private IBuildConfiguration currentBuildConfig;

	private TreeMap<String, IBuildConfiguration> configurations = new TreeMap<String, IBuildConfiguration>(String.CASE_INSENSITIVE_ORDER);

	private boolean isBuildConfigurationsSupported;

	private HashMap<String, SLD> slds = new HashMap<String, SLD>();

	private boolean disposed = false;

	private IBuildResultManager brManager = null;

    private HashMap<IBuildVariant, IBuildState> cachedBuildStates = new HashMap<IBuildVariant, IBuildState>();

    private HashMap<IPropertyOwner, PathExclusionFilter> excludes = new HashMap<IPropertyOwner, PathExclusionFilter>();
    
    private MoSyncProject(IProject project) {
        Assert.isNotNull(project);
        this.project = project;
        this.deviceFilterListener = new DeviceFilterListener();
        initFromProjectMetaData(null, SHARED_PROPERTY);
        initFromProjectMetaData(null, LOCAL_PROPERTY);
        addDeviceFilterListener();
        brManager = new BuildResultManager(this);
    }

    private void addDeviceFilterListener() {        
        deviceFilter.addPropertyChangeListener(deviceFilterListener);
    }
    
    private void removeDeviceFilterListener() {
        deviceFilter.removePropertyChangeListener(deviceFilterListener);
    }

    private void initFromProjectMetaData(IPath projectMetaDataPath, int store) {
    	if (projectMetaDataPath == null) {
    		projectMetaDataPath = getMoSyncProjectMetaDataLocation(store);
    	}
    	
        if (!projectMetaDataPath.toFile().exists()) {
            return;
        }
        
        FileReader input = null;
        
        try {
            // Older projects only have a shared config, so initialize everything
            // from that first
            input = new FileReader(projectMetaDataPath.toFile());
            XMLMemento memento = XMLMemento.createReadRoot(input);
            initTargetProfileFromProjectMetaData(memento);
            // Special case; device filters are always shared.
            if (store == SHARED_PROPERTY) {
            	deviceFilter = CompositeDeviceFilter.read(memento);
                initAvailableBuildConfigurations(memento);
            }
            initActiveBuildConfiguration(memento);

            initProperties(initPropertiesFromProjectMetaData(memento), store);
            
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

	private void initProperties(Map<String, String> properties, int store) {
    	if (store == LOCAL_PROPERTY) {
    		localProperties = properties;
    	} else {
    		sharedProperties = properties;
    	}
    	this.properties = new CascadingProperties(new Map[] { sharedProperties, localProperties });
    }
    
	private void initActiveBuildConfiguration(XMLMemento memento) {
		IMemento activeCfg = memento.getChild(ACTIVE_BUILD_CONFIG);
		if (activeCfg != null) {
			String activeCfgId = activeCfg.getString(ACTIVE_BUILD_CONFIG_KEY);
			currentBuildConfig = getBuildConfiguration(activeCfgId);
		}
	}
	
    private void initAvailableBuildConfigurations(XMLMemento memento) {
    	Boolean supportsBuildConfigurations = memento.getBoolean(BUILD_CONFIGS_SUPPORTED);
    	this.isBuildConfigurationsSupported = supportsBuildConfigurations == null ? false : supportsBuildConfigurations;
    	    	
    	configurations.clear();
    	IMemento[] cfgs = memento.getChildren(BUILD_CONFIG);
    	for (int i = 0; i < cfgs.length; i++) {
    		String id = cfgs[i].getString(BUILD_CONFIG_ID_KEY);
    		String[] cfgTypes = PropertyUtil.toStrings(cfgs[i].getString(BUILD_CONFIG_TYPES));
    		BuildConfiguration cfg = new BuildConfiguration(this, id, cfgTypes);
    		// For older versions of the mosync project, the active
    		// property might be set as an attribute here instead of
    		// as a separate tag.
    		if (Boolean.TRUE.equals(cfgs[i].getBoolean(ACTIVE_BUILD_CONFIG_KEY))) {
    			currentBuildConfig = cfg;
    		}
    		configurations.put(id, cfg);
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
    	updateProjectSpec(SHARED_PROPERTY);
    	updateProjectSpec(LOCAL_PROPERTY);
    }
    
    protected void updateProjectSpec(int store) {
        IPath projectMetaDataPath = getMoSyncProjectMetaDataLocation(store);
        XMLMemento root = XMLMemento.createWriteRoot(PROJECT);
        FileWriter output = null;

        try {
            output = new FileWriter(projectMetaDataPath.toFile());
           
            // Some properties are meant to be stored only in
            // shared properties, others only in local.
            if (store == SHARED_PROPERTY) {
            	saveAvailableBuildConfigurations(root);
            	deviceFilter.saveState(root);
            }
            
            if (store == LOCAL_PROPERTY) {
            	saveActiveBuildConfiguration(root);
                IMemento target = root.createChild(TARGET);
            	saveTargetProfile(target);	
            }
            
            IMemento propertiesMemento = root.createChild(PROPERTIES);
            saveProperties(propertiesMemento, getProperties(store));
    
            root.putString(VERSION_KEY, VERSION);
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
    
    private void saveActiveBuildConfiguration(XMLMemento root) {
		if (currentBuildConfig != null) {
	    	IMemento node = root.createChild(ACTIVE_BUILD_CONFIG);
			node.putString(ACTIVE_BUILD_CONFIG_KEY, currentBuildConfig.getId());
		}
	}

	private void saveAvailableBuildConfigurations(XMLMemento root) {
    	// Older versions of the project file format do not have build configs,
    	// so we save a marker denoting that we do.
    	root.putBoolean(BUILD_CONFIGS_SUPPORTED, isBuildConfigurationsSupported);
    	for (String cfgId : getBuildConfigurations()) {
    		IMemento node = root.createChild(BUILD_CONFIG);
    		node.putString(BUILD_CONFIG_ID_KEY, cfgId);
    		IBuildConfiguration cfg = getBuildConfiguration(cfgId);
    		node.putString(BUILD_CONFIG_TYPES, PropertyUtil.fromStrings(cfg.getTypes().toArray(new String[0])));
    	}    	
	}

	private void saveProperties(IMemento memento, Map<String, String> properties) {
		TreeMap<String, String> sortedProperties = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		sortedProperties.putAll(properties);
        for (Iterator<String> propertyIterator = sortedProperties.keySet().iterator(); propertyIterator.hasNext(); ) {
            String key = propertyIterator.next();
            String value = properties.get(key);
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

    public IPath getMoSyncProjectMetaDataLocation(int store) {
    	if (store == LOCAL_PROPERTY) {
    		return project.getLocation().append(MOSYNC_LOCAL_PROJECT_META_DATA_FILENAME);
    	} else {
    		return project.getLocation().append(MOSYNC_PROJECT_META_DATA_FILENAME);
    	}
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
     * Disposes of this mosyncproject, so subsequent calls
     * to <code>MoSyncProject.create(IProject)</code> will
     * return another <code>MoSyncProject</code> object.
     * Clients can, but should not, perform operations on
     * a disposed mosyncproject.
     */
    public void dispose() {
    	projects.remove(this);
    	disposed = true;
    }
    
    public boolean isDisposed() {
    	return disposed;
    }
    
    /**
     * <p>Returns a (shared) instance of a MoSyncProject of
     * the provided project, or <code>null</code> if
     * the project does not have a MoSync nature.</p>
     * <p>An optional project meta data file initialization file may
     * be provided. This file will then be loaded and its project meta
     * data used by the project.</p>
     * @param project The eclipse project this <code>MoSyncProject</code> should wrap
     * @param projectMetadataLocation The location of the (shared) project meta data. If <code>null</code>,
     * no initialization will take place unless this is the first call to <code>create</code> 
     * with the provided <code>project</code>.
     * @return
     */
    public static MoSyncProject create(IProject project, IPath projectMetadataLocation) {
    	MoSyncProject result = create(project);
        if (projectMetadataLocation != null) {
        	result.initFromProjectMetaData(projectMetadataLocation, SHARED_PROPERTY);
        	// There may be a local file too - we just try a reasonable default
        	IPath localProjectMetaDataLocation = 
        		projectMetadataLocation.removeLastSegments(1).append(MOSYNC_LOCAL_PROJECT_META_DATA_FILENAME);
        	result.initFromProjectMetaData(localProjectMetaDataLocation, LOCAL_PROPERTY);
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
        // TODO: A bit out of place, but it works
        excludes.clear();
        
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
        firePropertyChange(new PropertyChangeEvent(this, IDeviceFilter.FILTER_CHANGED, oldFilter, deviceFilter));
    }
    
    /**
     * <p>Returns the path to the STABS debug info file of a specific build configuration.</p>
     * @param buildConfiguration 
     * @return
     */
	public IPath getStabsPath(IBuildConfiguration buildConfiguration) {
	    IPath outputPath = MoSyncBuilder.getOutputPath(project, new BuildVariant(target, buildConfiguration == null ? null : buildConfiguration.getId(), false)).append("stabs.tab");
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
        IPath outputPath = MoSyncBuilder.getOutputPath(project, new BuildVariant(target, buildConfiguration == null ? null : buildConfiguration.getId(), false)).append("Sld.tab");
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
    
    /**
     * <p>Returns all properties of this project, including 
     * properties for all build configurations.</p>
     * <p>This method will return a copy of the properties, so the map returned may be freely modified by clients</p>
     * <p><b>NOTE:</b> Are you sure you want to use this method? You probably want to use getPropertyOwner(), which
     * will properly handle build configurations, etc.</code>
     */
    public Map<String, String> getProperties() {
    	return properties.toMap();
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
		IProfile[] profiles = MoSyncTool.getDefault().getProfiles(deviceFilter);
		return profiles;
	}

	/**
	 * Returns the default value of a project specific property
	 */
	public String getDefaultProperty(String key) {
		return CoreMoSyncPlugin.getDefault().getDefaultValue(this, key);
	}
	
	/**
	 * <p>Sets the property, regardless of any previous
	 * value.</p><p>Any property set by this method will
	 * be stored in the SHARED_PROPERTY properties.</p>
	 * @param key
	 * @param value If <code>null</code>, then the entry is removed  
	 */
	public void initProperty(String key, String value) {
		initProperty(key, value, SHARED_PROPERTY);
	}
	
	/**
	 * <p>Sets the property, regardless of any previous
	 * value.</p> 
	 * @param key
	 * @param value If <code>null</code>, then the entry is removed  
	 * @param store The store that this key should be set in, ie LOCAL
	 * or SHARED
	 */
	public void initProperty(String key, String value, int store) {
		if (value == null) {
			getProperties(store).remove(key);
		} else {
			getProperties(store).put(key, value);
		}		
	}
	
	private Map<String, String> getProperties(int store) {
		if (store == LOCAL_PROPERTY) {
			return localProperties;
		} else {
			return sharedProperties;
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
		
		IPropertyOwner properties = project.getPropertyOwner();
		if (project.excludes.get(properties) == null) {		    
    		// TODO: Efficient - well, not really... And this method is heavily used!
    		String[] standardExclusion = PropertyUtil.getStrings(properties, STANDARD_EXCLUDES_FILTER_KEY);
    		String[] exclusions = PropertyUtil.getStrings(properties, EXCLUDE_FILTER_KEY);
    		String[] aggregateExclusions = new String[standardExclusion.length + exclusions.length];
    		System.arraycopy(standardExclusion, 0, aggregateExclusions, 0, standardExclusion.length);
    		System.arraycopy(exclusions, 0, aggregateExclusions, standardExclusion.length, exclusions.length);
    		Map<String, String> params = properties.getProperties();
    		
    		ArrayList<String> finalExclusions = new ArrayList<String>();
    		// TODO: All params should be able to have % tags.
    		for (int i = 0; i < aggregateExclusions.length; i++) {
    		    String excluded = Util.replace(aggregateExclusions[i], params);
    		    String[] excludedPaths = PropertyUtil.toStrings(excluded);
    		    finalExclusions.addAll(Arrays.asList(excludedPaths));
    		}
    		
    		project.excludes.put(properties, PathExclusionFilter.parse(finalExclusions.toArray(new String[0])));
		}
		
		return project.excludes.get(properties);
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
     * <p>Adds all properties of <code>properties</code> to this project.</p>
     * <p>They will all be added to the SHARED store.</p>
     * @param properties
     */
    public void setProperties(Map<String, String> properties) {
        sharedProperties.putAll(properties);
        updateProjectSpec();
    }
    
    /**
     * <p>Returns the build state for a variant manager of this project.
     * All non-finalizer build states may be cached.</p>
     * @return
     */
	public IBuildState getBuildState(IBuildVariant variant) {
	    IBuildState result = cachedBuildStates.get(variant);
	    boolean wasNull = true; //result == null;
        if (wasNull) {
            result = new BuildState(this, variant);
        }
        
	    if (wasNull && !variant.isFinalizerBuild()) {
            cachedBuildStates.put(variant, result);   
	    }
	    
		return result;
	}

	/**
	 * Returns the current build configuration. If none is assigned,
	 * this method will try to assign a default build configuration
	 * (<code>IBuildConfiguration.RELEASE_ID</code>). If the default build configuration does
	 * not exist, <code>null</code> will be returned.
	 * This method will return a value regardless of what
	 * <code>isBuildConfigurationsSupported</code> returns.
	 * @return
	 */
	public IBuildConfiguration getActiveBuildConfiguration() {
	    if (currentBuildConfig == null) {
	        currentBuildConfig = getBuildConfiguration(IBuildConfiguration.RELEASE_ID);
	    }
	    
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
	
	/**
	 * Returns the ids of all build configurations that has
	 * a set of types
	 * @param types The types to match against
	 * @return The build configurations that have <b>all</b> the specified types 
	 */
    public Set<IBuildConfiguration> getBuildConfigurations(String... types) {
        HashSet<IBuildConfiguration> result = new HashSet<IBuildConfiguration>();
        for (IBuildConfiguration cfg : configurations.values()) {
            boolean doAdd = true;
            for (int i = 0; i < types.length; i++) {
                doAdd &= cfg.getTypes().contains(types[i]);
            }
            
            if (doAdd) {
                result.add(cfg);
            }
        }
        
        return result;
    }

	/**
	 * Returns the build configuration for a given id
	 * @param id
	 * @return <code>null</code> if the given id is <code>null</code>
	 * or if there is no build configuration with that id
	 */
	public IBuildConfiguration getBuildConfiguration(String id) {
	    if (id == null) {
	        return null;
	    }
		return configurations.get(id);
	}
	
	public IBuildConfiguration installBuildConfiguration(String id, String[] types) {
		BuildConfiguration newConfig = new BuildConfiguration(this, id, types);
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
	
	public boolean areBuildConfigurationsSupported() {
		return isBuildConfigurationsSupported;
	}
	
	/**
	 * Activates build configurations for this project.
	 * If there already are installed build configurations,
	 * this amounts to calling <code>setBuildConfigurationsSupported(true);</code>,
	 * otherwise a default set of build configurations are installed.
	 */
	public void activateBuildConfigurations() {
		setBuildConfigurationsSupported(true);
		
		if (configurations.isEmpty()) {
			configurations.put("Release", new BuildConfiguration(this, IBuildConfiguration.RELEASE_ID, IBuildConfiguration.RELEASE_TYPE));
			configurations.put("Debug", new BuildConfiguration(this, IBuildConfiguration.DEBUG_ID, IBuildConfiguration.DEBUG_TYPE));
			setActiveBuildConfiguration("Release");
		}
	}

	public IPropertyOwner getPropertyOwner() {
		if (isBuildConfigurationsSupported && currentBuildConfig != null) {
			return currentBuildConfig.getProperties();
		}
		
		return this;
	}


	public LibraryLookup getLibraryLookup(IPropertyOwner buildProperties) {
		// TODO: cache?
		return new LibraryLookup(MoSyncBuilder.getLibraryPaths(getWrappedProject(), buildProperties), MoSyncBuilder.getLibraries(buildProperties));
	}

	public IBuildResultManager getBuildResults() {
		return brManager;
	}



}
