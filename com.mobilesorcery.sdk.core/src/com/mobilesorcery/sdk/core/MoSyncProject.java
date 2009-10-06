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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

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
import com.mobilesorcery.sdk.internal.PathExclusionFilter;
import com.mobilesorcery.sdk.internal.ProfileInfoParser;
import com.mobilesorcery.sdk.internal.SLD;
import com.mobilesorcery.sdk.internal.SLDParser;
import com.mobilesorcery.sdk.internal.dependencies.DependencyManager;
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
    
    private static final String NO_CACHE_SLD_KEY = "no.cache.sld";

    private static IdentityHashMap<IProject, MoSyncProject> projects = new IdentityHashMap<IProject, MoSyncProject>();
    
    private IProject project;

    private IProfile target;

    private static PropertyChangeSupport globalListeners = new PropertyChangeSupport(new Object());
    
    private PropertyChangeSupport listeners = new PropertyChangeSupport(this);

    private ICompositeDeviceFilter deviceFilter = new CompositeDeviceFilter(new IDeviceFilter[0]);
    
    private Map<String, String> properties = new HashMap<String, String>();

    private DeviceFilterListener deviceFilterListener;

	private DependencyManager<IResource> dependencyManager = new DependencyManager<IResource>();

	private long lastSLDTimestamp;

	private SLD lastSLD;

    private MoSyncProject(IProject project) {
        Assert.isNotNull(project);
        this.project = project;
        this.deviceFilterListener = new DeviceFilterListener(); 
        initFromProjectMetaData();        
        addDeviceFilterListener();
    }

    private void addDeviceFilterListener() {        
        deviceFilter.addPropertyChangeListener(deviceFilterListener);
    }
    
    private void removeDeviceFilterListener() {
        deviceFilter.removePropertyChangeListener(deviceFilterListener);
    }

    private void initFromProjectMetaData() {
        IPath projectMetaDataPath = computeMoSyncProjectLocation();
        if (!projectMetaDataPath.toFile().exists()) {
            return;
        }
        
        FileReader input = null;
        
        try {
            input = new FileReader(projectMetaDataPath.toFile());
            XMLMemento memento = XMLMemento.createReadRoot(input);
            initTargetProfileFromProjectMetaData(memento);
            
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
        IPath projectMetaDataPath = computeMoSyncProjectLocation();
        XMLMemento root = XMLMemento.createWriteRoot(PROJECT);
        FileWriter output = null;

        try {
            output = new FileWriter(projectMetaDataPath.toFile());
            IMemento target = root.createChild(TARGET);
            saveTargetProfile(target);
            
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
    
    private void saveProperties(IMemento memento) {
        for (Iterator properties = this.properties.keySet().iterator(); properties.hasNext(); ) {
            String key = (String) properties.next();
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

    private IPath computeMoSyncProjectLocation() {
        return project.getLocation().append(".mosyncproject");
    }

    /**
     * Returns a (shared) instance of a MoSyncProject of
     * the provided project, or <code>null</code> if
     * the project does not have a MoSync nature.
     * @param project
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
        IProfile oldTarget = this.target;
        this.target = newTarget;
        updateProjectSpec();
        firePropertyChange(new PropertyChangeEvent(this, TARGET_PROFILE_CHANGED, oldTarget, newTarget));
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
     * <p>Returns the path to the (single) SLD file of this project.</p>
     * <p>The SLD file maps addresses to files and line numbers.</p> 
     * @return
     */
    public IPath getSLDPath() {
        return getWrappedProject().getLocation().append("Sld.tab");        
    }
    
    /**
     * <p>Returns the path to the (single) STABS debug info file of this project.</p>
     * @return
     */
	public IPath getStabsPath() {
		return getWrappedProject().getLocation().append("stabs.tab");
	}

	/**
	 * <p>Parses the SLD of this project; equivalent to
	 * <code>parseSLD(false)</code></p>
	 * @return
	 */
    public SLD parseSLD() {
    	return parseSLD(false);
    }
    
    /**
     * Parses the SLD of this project if the SLD
     * file has a newer time stamp than when last parsed,
     * or if <code>force</code> is set to <code>true</code>.
     * If the project property defined by NO_CACHE_SLD_KEY is
     * set to <code>true</code>, parsing will always take place.
     * @param force
     * @return
     */
    public synchronized SLD parseSLD(boolean force) {
        IPath sld = getSLDPath();
        if (!sld.toFile().exists()) {
            return null;
        }
        
        boolean dontCache = Boolean.parseBoolean(getProperty(NO_CACHE_SLD_KEY));
        boolean alwaysParse = force || dontCache; 
        
        SLD result = dontCache ? null : lastSLD;
        
        long currentSLDTimestamp = sld.toFile().lastModified();
        boolean timestampChanged = lastSLDTimestamp != currentSLDTimestamp;
        boolean hasNoCached = lastSLD == null;
        
        boolean doParse = alwaysParse || hasNoCached || timestampChanged;
        
        if (CoreMoSyncPlugin.getDefault().isDebugging()) {
        	if (doParse) {
        		CoreMoSyncPlugin.trace("Parsing SLD: force = {0}, nocache = {1}, dirty = {2}", alwaysParse, hasNoCached, timestampChanged);
        	}
        }
        
        if (doParse) {
	        SLDParser parser = new SLDParser();
	        try {
	            parser.parse(sld.toFile());
	            result = parser.getSLD();
	            if (!dontCache) { 
	            	lastSLD = result;
	            	lastSLDTimestamp = currentSLDTimestamp; 
	            }
	        } catch (IOException e) {
	            // Ignore.
	        	e.printStackTrace();
	            CoreMoSyncPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, "Could not parse SLD file", e));
	        }
        }
        
        return result;
    }

    /**
     * Parses the profile info file for a project,
     * and returns 
     * @return
     * @throws IOException 
     * @throws ParseException 
     */
    public IProfileInfo parsePerformanceInfo(IPath profileInfoFile, IProgressMonitor monitor) throws ParseException, IOException {
    	monitor.beginTask("Parsing profile info", 2);
    	SLD sld = parseSLD();
    	monitor.worked(1);
    	IProfileInfo result = ProfileInfoParser.parse(profileInfoFile, sld, monitor);
    	monitor.done();
    	return result;
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
     * <p>Sets a project-specific property.</p>
     * @param key 
     * @param value
     * @return <code>true</code> if and only if the property was changed
     */
    public boolean setProperty(String key, String value) {
    	String oldValue = getProperty(key);
    	if (equals(oldValue, value)) {
    		return false;
    	}
    	
    	initProperty(key, value);

        updateProjectSpec();
        firePropertyChange(new PropertyChangeEvent(this, key, oldValue, value));
        
        return true;
    }

	private static boolean equals(String oldValue, String value) {
		if (oldValue == null) {
			return value == null;
		}
		
		return oldValue.equals(value);
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
	
	public IFilter<IResource> getExclusionFilter() {
		// Efficient?
		return PathExclusionFilter.parse(PropertyUtil.getStrings(this, EXCLUDE_FILTER_KEY));
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


}
