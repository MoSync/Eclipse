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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.core.resources.FileInfoMatcherDescription;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceFilterDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;

import com.mobilesorcery.sdk.core.security.IApplicationPermissions;
import com.mobilesorcery.sdk.internal.BuildState;
import com.mobilesorcery.sdk.internal.SecureProperties;
import com.mobilesorcery.sdk.internal.convert.MoSyncProjectConverter1_2;
import com.mobilesorcery.sdk.internal.convert.MoSyncProjectConverter1_4;
import com.mobilesorcery.sdk.internal.convert.MoSyncProjectConverter1_7;
import com.mobilesorcery.sdk.internal.dependencies.LibraryLookup;
import com.mobilesorcery.sdk.internal.security.ApplicationPermissions;
import com.mobilesorcery.sdk.profiles.ICompositeDeviceFilter;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.ITargetProfileProvider;
import com.mobilesorcery.sdk.profiles.IVendor;
import com.mobilesorcery.sdk.profiles.ProfileDBManager;
import com.mobilesorcery.sdk.profiles.filter.AbstractDeviceFilter;
import com.mobilesorcery.sdk.profiles.filter.CompositeDeviceFilter;
import com.mobilesorcery.sdk.profiles.filter.DeviceCapabilitiesFilter;

/**
 * This is a wrapper to provider mosync-specific capabilities to a vanilla
 * IProject, eg special properties, persistence, etc
 *
 * @author Mattias
 *
 */
public class MoSyncProject extends PropertyOwnerBase implements
		ITargetProfileProvider {

	/**
	 * An interface for converting older {@link MoSyncProject}s into a newer
	 * format.
	 *
	 * @author Mattias Bybro
	 *
	 */
	public interface IConverter {

		/**
		 * Converts an older project into a newer format.
		 *
		 * @param project
		 * @throws CoreException
		 */
		public void convert(MoSyncProject project) throws CoreException;

	}

	public final static Comparator<MoSyncProject> NAME_COMPARATOR = new Comparator<MoSyncProject>() {

		@Override
		public int compare(MoSyncProject p1, MoSyncProject p2) {
			return p1.getName().compareTo(p2.getName());
		}

	};

	private final class DeviceFilterListener implements PropertyChangeListener {
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			updateProjectSpec();
			// Then just pass it along.
			firePropertyChange(event);
		}
	}

	/**
	 * The property change event type that is triggered if the target profile of
	 * this project changes
	 */
	public static final String TARGET_PROFILE_CHANGED = "target.profile";

	/**
	 * The property change event type that is triggered if the build
	 * configuration of this project changes
	 */
	public static final String BUILD_CONFIGURATION_CHANGED = "build.config";

	/**
	 * The property change event type that is triggered if the build
	 * configuration support of this project changes
	 */
	public static final String BUILD_CONFIGURATION_SUPPORT_CHANGED = "build.config.support";

	/**
	 * The file exclude filter property for this project.
	 *
	 * @see PathExclusionFilter
	 */
	public static final String EXCLUDE_FILTER_KEY = "excludes";

	/**
	 * The standard file excludes for this project (usually NOT an a
	 * per-configuration basis).
	 *
	 * @deprecated Move this to the testing plugin
	 */
	@Deprecated
	public static final String STANDARD_EXCLUDES_FILTER_KEY = "standard.excludes";

	/**
	 * The key for the profile manager type associated with this project.
	 */
	public static final String PROFILE_MANAGER_TYPE_KEY = "profile.mgr.type";

	/**
	 * The project type of MoSync Projects (used only by CDT).
	 */
	public static final String C_PROJECT_ID = CoreMoSyncPlugin.PLUGIN_ID
			+ ".project";

	/**
	 * The property initializer context of MoSyncProjects
	 *
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

	/**
	 * The extension of the XML files that describes the icons used in a
	 * project.
	 */
	public static final String ICON_FILE_EXTENSION = ".icon";

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

	/**
	 * The format version currently used to store projects.
	 *
	 * @see MoSyncProject#getFormatVersion()
	 */
	public static final Version CURRENT_VERSION = new Version("1.7");

	private static final Version VERSION_1_0 = new Version("1");

	/**
	 * The name of the file where this project's <b>shared</b> meta data is
	 * located.
	 */
	public static final String MOSYNC_PROJECT_META_DATA_FILENAME = ".mosyncproject";

	/**
	 * The name of the file where this project's <b>local</b> meta data is
	 * located.
	 */
	public static final String MOSYNC_LOCAL_PROJECT_META_DATA_FILENAME = ".mosyncproject.local";

	private static final int SHARED_PROPERTY = 0;

	static final int LOCAL_PROPERTY = 1;

	static final int WORKSPACE_LOCAL_PROPERTY = 2;

	private static IdentityHashMap<IProject, MoSyncProject> projects = new IdentityHashMap<IProject, MoSyncProject>();

	private final IProject project;

	private IProfile target;

	private static PropertyChangeSupport globalListeners = new PropertyChangeSupport(
			new Object());

	private final PropertyChangeSupport listeners = new PropertyChangeSupport(
			this);

	private ICompositeDeviceFilter deviceFilter = new CompositeDeviceFilter(
			new IDeviceFilter[0]);

	/**
	 * This is the map holding the shared properties of this project, stored in
	 * .mosyncproject
	 */
	private final Map<String, String> sharedProperties = new HashMap<String, String>();

	/**
	 * This is the map holding the user's local properties of this project,
	 * stored in .mosyncproject.local
	 */
	private final Map<String, String> localProperties = new HashMap<String, String>();

	private final Map<String, String> workspaceLocalProperties = new HashMap<String, String>();

	private final CascadingProperties properties = new CascadingProperties(
			new Map[] { sharedProperties, localProperties,
					workspaceLocalProperties });

	private final DeviceFilterListener deviceFilterListener;

	private IBuildConfiguration currentBuildConfig;

	private final TreeMap<String, IBuildConfiguration> configurations = new TreeMap<String, IBuildConfiguration>(
			String.CASE_INSENSITIVE_ORDER);

	private boolean isBuildConfigurationsSupported;

	private final HashMap<String, SLD> slds = new HashMap<String, SLD>();

	private boolean disposed = false;

	private final HashMap<IBuildVariant, IBuildState> cachedBuildStates = new HashMap<IBuildVariant, IBuildState>();

	private final HashMap<IPropertyOwner, PathExclusionFilter> excludes = new HashMap<IPropertyOwner, PathExclusionFilter>();

	private final ApplicationPermissions permissions;

	private Version formatVersion = CURRENT_VERSION;

	private final ISecurePropertyOwner securePropertyOwner;

	private MoSyncProject(IProject project) {
		Assert.isNotNull(project);
		this.project = project;
		this.deviceFilterListener = new DeviceFilterListener();
		reinit(false);
		permissions = new ApplicationPermissions(this);
		addDeviceFilterListener();
		securePropertyOwner = new SecureProperties(this, CoreMoSyncPlugin
				.getDefault().getPasswordProvider(),
				SecureProperties.DEFAULT_SECURE_PROPERTY_SUFFIX);
	}

	private void addDeviceFilterListener() {
		deviceFilter.addPropertyChangeListener(deviceFilterListener);
	}

	private void removeDeviceFilterListener() {
		deviceFilter.removePropertyChangeListener(deviceFilterListener);
	}

	/**
	 * Reinitializes this project with the settings file(s). This
	 * enables external programs to modify the setting file(s) and
	 * this project to synchronize with the files.
	 */
	public void reinit(boolean fireEvents) {
		Map<String, String> oldProperties = getProperties();
		initFromProjectMetaData(null, SHARED_PROPERTY);
		initFromProjectMetaData(null, LOCAL_PROPERTY);
		initFromProjectMetaData(null, WORKSPACE_LOCAL_PROPERTY);

		if (fireEvents) {
			Map<String, String> newProperties = getProperties();
			Set<String> changedProperties = BuildState.getPropertiesDiff(oldProperties, newProperties);

			for (String changedProperty : changedProperties) {
				Object oldValue = oldProperties.get(changedProperty);
				Object newValue = newProperties.get(changedProperty);
				firePropertyChange(new PropertyChangeEvent(this, changedProperty, oldValue, newValue));
			}
			if (!changedProperties.isEmpty()) {
				invalidatePropertyDependentObjects();
				setProfileManagerType(getProfileManagerType(), true);
			}
		}
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
			// Older projects only have a shared config, so initialize
			// everything
			// from that first
			input = new FileReader(projectMetaDataPath.toFile());
			XMLMemento memento = XMLMemento.createReadRoot(input);
			String formatVersionStr = memento.getString(VERSION_KEY);
			formatVersion = formatVersionStr == null ? VERSION_1_0
					: new Version(formatVersionStr);

			// Special case; device filters are always shared.
			if (store == SHARED_PROPERTY) {
				deviceFilter = CompositeDeviceFilter.read(memento);
				initAvailableBuildConfigurations(memento);
			}
			initActiveBuildConfiguration(memento);

			initProperties(initPropertiesFromProjectMetaData(memento), store);

			initTargetProfileFromProjectMetaData(memento);
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
		Map<String, String> oldProperties = getProperties(store);
		oldProperties.clear();
		oldProperties.putAll(properties);
		invalidatePropertyDependentObjects();
	}

	// Makes sure that objects that are directly dependent
	// on properties are invalidated and re-initialized
	private void invalidatePropertyDependentObjects() {
		if (permissions != null) {
			permissions.refresh();
		}
	}

	private void initActiveBuildConfiguration(XMLMemento memento) {
		IMemento activeCfg = memento.getChild(ACTIVE_BUILD_CONFIG);
		if (activeCfg != null) {
			String activeCfgId = activeCfg.getString(ACTIVE_BUILD_CONFIG_KEY);
			currentBuildConfig = getBuildConfiguration(activeCfgId);
		}
	}

	private void initAvailableBuildConfigurations(XMLMemento memento) {
		Boolean supportsBuildConfigurations = memento
				.getBoolean(BUILD_CONFIGS_SUPPORTED);
		this.isBuildConfigurationsSupported = supportsBuildConfigurations == null ? false
				: supportsBuildConfigurations;

		configurations.clear();
		IMemento[] cfgs = memento.getChildren(BUILD_CONFIG);
		for (int i = 0; i < cfgs.length; i++) {
			String id = cfgs[i].getString(BUILD_CONFIG_ID_KEY);
			String[] cfgTypes = PropertyUtil.toStrings(cfgs[i]
					.getString(BUILD_CONFIG_TYPES));
			BuildConfiguration cfg = new BuildConfiguration(this, id, cfgTypes);
			// For older versions of the mosync project, the active
			// property might be set as an attribute here instead of
			// as a separate tag.
			if (Boolean.TRUE
					.equals(cfgs[i].getBoolean(ACTIVE_BUILD_CONFIG_KEY))) {
				currentBuildConfig = cfg;
			}
			configurations.put(id, cfg);
		}
	}

	private Map<String, String> initPropertiesFromProjectMetaData(
			XMLMemento memento) {
		Map<String, String> result = new HashMap<String, String>();
		IMemento properties = memento.getChild(PROPERTIES);
		if (properties != null) {
			IMemento[] propertyChildren = properties.getChildren(PROPERTY);
			for (int i = 0; propertyChildren != null
					&& i < propertyChildren.length; i++) {
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
			IVendor vendor = vendorName == null ? null : getProfileManager().getVendor(vendorName);
			if (vendor != null) {
				IProfile targetProfile = vendor.getProfile(profileName);
				if (targetProfile != null) {
					target = targetProfile;
				}
			}
		}
	}

	public void updateProjectSpec() {
		updateProjectSpec(SHARED_PROPERTY);
		updateProjectSpec(LOCAL_PROPERTY);
		updateProjectSpec(WORKSPACE_LOCAL_PROPERTY);
	}

	protected void updateProjectSpec(int store) {
		if (!requiresUpdate(store)) {
			return;
		}
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

			if (store == SHARED_PROPERTY) {
				root.putString(VERSION_KEY, formatVersion.asCanonicalString());
			}
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

	private boolean requiresUpdate(int store) {
		IPath projectMetaDataPath = getMoSyncProjectMetaDataLocation(store);
		// Special case: the local properties; if it exists, we always write it
		// anew regardless.
		return store != WORKSPACE_LOCAL_PROPERTY
				|| projectMetaDataPath.toFile().exists()
				|| !getProperties(store).isEmpty();
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
			node.putString(
					BUILD_CONFIG_TYPES,
					PropertyUtil.fromStrings(cfg.getTypes().toArray(
							new String[0])));
		}
	}

	private void saveProperties(IMemento memento, Map<String, String> properties) {
		TreeMap<String, String> sortedProperties = new TreeMap<String, String>(
				String.CASE_INSENSITIVE_ORDER);
		sortedProperties.putAll(properties);
		for (Iterator<String> propertyIterator = sortedProperties.keySet()
				.iterator(); propertyIterator.hasNext();) {
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
		switch (store) {
		case SHARED_PROPERTY:
			return project.getLocation().append(
					MOSYNC_PROJECT_META_DATA_FILENAME);
		case LOCAL_PROPERTY:
			return project.getLocation().append(
					MOSYNC_LOCAL_PROJECT_META_DATA_FILENAME);
		case WORKSPACE_LOCAL_PROPERTY:
			return project
					.getLocation()
					.append(MOSYNC_LOCAL_PROJECT_META_DATA_FILENAME + "."
							+ CoreMoSyncPlugin.getDefault().getWorkspaceToken());
		default:
			throw new IllegalArgumentException();
		}
	}

	/**
	 * <p>
	 * Returns a (shared) instance of a MoSyncProject of the provided project,
	 * or <code>null</code> if the project does not have a MoSync nature.
	 * </p>
	 *
	 * @param project
	 *            The eclipse project this <code>MoSyncProject</code> should
	 *            wrap
	 * @return
	 */
	public static MoSyncProject create(IProject project) {
		try {
			if (!MoSyncNature.isCompatible(project)) {
				return null;
			}

			boolean upgrade = false;
			MoSyncProject result = null;

			synchronized (projects) {
				result = projects.get(project);
				if (result == null) {
					result = new MoSyncProject(project);
					projects.put(project, result);
					upgrade = !CURRENT_VERSION
							.equals(result.getFormatVersion());
				}
			}

			if (upgrade) {
				upgrade(result);
			}

			return result;
		} catch (CoreException e) {
			return null;
		}
	}
	

	/**
	 * Extracts all {@code MoSyncProject}s that contains any one of the
	 * provided {@code IResource}s.
	 * @param projects
	 * @return A list of {@code MoSyncProject}s, which may be empty but never {@code null}
	 */
	public static List<MoSyncProject> create(List<IResource> resources) {
		ArrayList<MoSyncProject> result = new ArrayList<MoSyncProject>();
		HashSet<IProject> alreadyCreated = new HashSet<IProject>();
		for (IResource resource : resources) {
			IProject project = resource.getProject();
			if (!alreadyCreated.contains(project)) {
				alreadyCreated.add(project);
				MoSyncProject mosyncProject = MoSyncProject.create(project);
				if (project != null) {
					result.add(mosyncProject);
				}
			}
		}
		return result;
	}

	private static void upgrade(MoSyncProject project) throws CoreException {
		// TODO: Whenever the need arises we may want to fix something smarter
		MoSyncProjectConverter1_2.getInstance().convert(project);
		MoSyncProjectConverter1_4.getInstance().convert(project);
		MoSyncProjectConverter1_7.getInstance().convert(project);
		project.setFormatVersion(CURRENT_VERSION);
	}

	public static void addCapabilityFilters(MoSyncProject result, String[] requiredCapabilities) {
		if (result.getProfileManagerType() == MoSyncTool.DEFAULT_PROFILE_TYPE) {
			result.getDeviceFilter().addFilter(
				DeviceCapabilitiesFilter.create(requiredCapabilities, new String[0]));
		}
	}

	public static void addDefaultResourceFilter(IProject project,
			IProgressMonitor monitor) throws CoreException {
		// TODO: Hmmm.... maybe we should consider filtering out output folders?
		// FileInfoMatcherDescription filter = new
		// FileInfoMatcherDescription("org.eclipse.core.resources.regexFilterMatcher",
		// ".*rebuild.build.cpp");
		// Very internal format, but the version number should help us be a bit
		// future proof.
		FileInfoMatcherDescription filter = new FileInfoMatcherDescription(
				"org.eclipse.ui.ide.multiFilter",
				"1.0-name-matches-false-true-.*rebuild.build.cpp");
		IResourceFilterDescription created = project.createFilter(
				IResourceFilterDescription.EXCLUDE_ALL
						| IResourceFilterDescription.FILES, filter, 0, monitor);
	}

	/**
	 * Disposes of this mosyncproject, so subsequent calls to
	 * <code>MoSyncProject.create(IProject)</code> will return another
	 * <code>MoSyncProject</code> object. Clients can, but should not, perform
	 * operations on a disposed mosyncproject.
	 */
	public void dispose() {
		synchronized (projects) {
			projects.remove(this.project);
		}

		disposed = true;
	}

	public boolean isDisposed() {
		return disposed;
	}

	/**
	 * <p>
	 * Returns a (shared) instance of a MoSyncProject of the provided project,
	 * or <code>null</code> if the project does not have a MoSync nature.
	 * </p>
	 * <p>
	 * An optional project meta data file initialization file may be provided.
	 * This file will then be loaded and its project meta data used by the
	 * project.
	 * </p>
	 *
	 * @param project
	 *            The eclipse project this <code>MoSyncProject</code> should
	 *            wrap
	 * @param projectMetadataLocation
	 *            The location of the (shared) project meta data. If
	 *            <code>null</code>, no initialization will take place unless
	 *            this is the first call to <code>create</code> with the
	 *            provided <code>project</code>.
	 * @return
	 */
	public static MoSyncProject create(IProject project,
			IPath projectMetadataLocation) {
		MoSyncProject result = create(project);
		if (projectMetadataLocation != null) {
			// We only imported SHARED properties.
			result.initFromProjectMetaData(projectMetadataLocation,
					SHARED_PROPERTY);
			// ... but MOSYNCTWOSIX-344 showed we need the local
			// ones too
			IPath localMetaDataLocation = projectMetadataLocation
					.removeLastSegments(1).append(
							MOSYNC_LOCAL_PROJECT_META_DATA_FILENAME);
			result.initFromProjectMetaData(localMetaDataLocation,
					LOCAL_PROPERTY);
		}
		result.updateProjectSpec();
		return result;
	}

	/**
	 * <p>
	 * Returns a (shared) instance of a MoSyncProject that has a given name, or
	 * <code>null</code> if no such project exists.
	 * </p>
	 */
	public static MoSyncProject create(String name) {
		IProject project = ResourcesPlugin.getPlugin().getWorkspace().getRoot()
				.getProject(name);
		return project == null ? null : create(project);
	}

	/**
	 * <p>
	 * Returns the current target profile of this MoSync project.
	 * </p>
	 * <p>
	 * If none is set, a default target profile is returned.
	 * </p>
	 */
	@Override
	public IProfile getTargetProfile() {
		return target == null ? getProfileManager().getDefaultTargetProfile() : target;
	}

	/**
	 * <p>
	 * Sets the current target profile of this MoSync project, and notifies all
	 * listeners about this change.
	 * </p>
	 *
	 * @param newTarget
	 *            The new target profile
	 */
	public void setTargetProfile(IProfile newTarget) {
		IProfile oldTarget = initTargetProfile(newTarget);
		firePropertyChange(new PropertyChangeEvent(this,
				TARGET_PROFILE_CHANGED, oldTarget, getTargetProfile()));
	}

	/**
	 * Initializes the target profile; same as <code>setTargetProfile</code>,
	 * but no event is fired.
	 *
	 * @param newTarget
	 * @return The old target profile.
	 */
	public IProfile initTargetProfile(IProfile newTarget) {
		IProfile oldTarget = getTargetProfile();
		this.target = newTarget;
		updateProjectSpec();
		return oldTarget;
	}

	private void firePropertyChange(PropertyChangeEvent event) {
		// TODO: A bit out of place, but it works
		excludes.clear();

		try {
			globalListeners.firePropertyChange(event);
			listeners.firePropertyChange(event);
		} catch (Exception e) {
			// Continue anyway.
			CoreMoSyncPlugin.getDefault().log(e);
		}
	}

	/**
	 * <p>
	 * Adds a <emph>global</emph> property listener, ie a listener that listens
	 * to changes to <emph>all</emph> MoSync projects in a workspace.
	 * </p>
	 *
	 * @param globalListener
	 *            The listener to add
	 */
	public static void addGlobalPropertyChangeListener(
			PropertyChangeListener globalListener) {
		globalListeners.addPropertyChangeListener(globalListener);
	}

	/**
	 * <p>
	 * Removes a <emph>global</emph> property listener, ie a listener that
	 * listens to changes to <emph>all</emph> MoSync projects in a workspace.
	 * </p>
	 *
	 * @param globalListener
	 *            The listener to remove
	 */
	public static void removeGlobalPropertyChangeListener(
			PropertyChangeListener globalListener) {
		globalListeners.removePropertyChangeListener(globalListener);
	}

	/**
	 * <p>
	 * Adds a property listener to this project.
	 * </p>
	 * <p>
	 * Property listeners are notified about events such as changing target
	 * profile or any other project property change set by
	 * <code>setProperty</code>.
	 *
	 * @param globalListener
	 *            The listener to add
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		listeners.addPropertyChangeListener(listener);
	}

	/**
	 * <p>
	 * Removes a property listener from this project.
	 * </p>
	 *
	 * @param globalListener
	 *            The listener to remove
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		listeners.removePropertyChangeListener(listener);
	}

	/**
	 * <p>
	 * Returns the <code>ICompositeDeviceFilter</code> currently associated with
	 * this project.
	 * </p>
	 *
	 * @return
	 */
	public ICompositeDeviceFilter getDeviceFilter() {
		return deviceFilter;
	}

	/**
	 * <p>
	 * Sets the <code>ICompositeDeviceFilter</code> currently associated with
	 * this project.
	 * </p>
	 *
	 * @param deviceFilter
	 *            The new filter
	 */
	public void setDeviceFilter(ICompositeDeviceFilter deviceFilter) {
		ICompositeDeviceFilter oldFilter = this.deviceFilter;
		removeDeviceFilterListener();
		this.deviceFilter = deviceFilter;
		addDeviceFilterListener();
		firePropertyChange(new PropertyChangeEvent(this,
				IDeviceFilter.FILTER_CHANGED, oldFilter, deviceFilter));
	}

	/**
	 * <p>
	 * Returns the path to the STABS debug info file of a specific build
	 * configuration.
	 * </p>
	 *
	 * @param buildConfiguration
	 * @return
	 */
	public IPath getStabsPath(IBuildConfiguration buildConfiguration) {
		IPath outputPath = MoSyncBuilder.getOutputPath(
				project,
				new BuildVariant(getTargetProfile(), buildConfiguration == null ? null
						: buildConfiguration.getId())).append("stabs.tab");
		return outputPath;
	}

	/**
	 * Returns the SLD for a specific buildconfiguration; if a null build
	 * configuration is passed as argument, then this amounts to build
	 * configurations not being supported.
	 *
	 * @param buildConfiguration
	 * @return
	 */
	public synchronized SLD getSLD(IBuildConfiguration buildConfiguration) {
		IPath outputPath = MoSyncBuilder.getOutputPath(
				project,
				new BuildVariant(getTargetProfile(), buildConfiguration == null ? null
						: buildConfiguration.getId())).append("Sld.tab");
		SLD sld = slds.get(outputPath.toPortableString());
		if (sld == null) {
			sld = new SLD(this, outputPath);
			slds.put(outputPath.toPortableString(), sld);
		}

		return sld;
	}

	/**
	 * Returns the underlying Eclipse project of this MoSync project.
	 *
	 * @return
	 */
	public IProject getWrappedProject() {
		return project;
	}

	/**
	 * <p>
	 * Returns a project-specific property, using default properties set by
	 * property initializers if necessary.
	 * </p>
	 */
	@Override
	public String getProperty(String key) {
		String property = properties.get(key);
		if (property == null) {
			return getDefaultProperty(key);
		}

		return property;
	}

	/**
	 * <p>
	 * Returns all properties of this project, including properties for all
	 * build configurations.
	 * </p>
	 * <p>
	 * This method will return a copy of the properties WITHOUT default values,
	 * so the map returned may be freely modified by clients
	 * </p>
	 * <p>
	 * <b>NOTE:</b> Are you sure you want to use this method? You probably want
	 * to use getPropertyOwner(), which will properly handle build
	 * configurations, etc.</code>
	 */
	@Override
	public Map<String, String> getProperties() {
		return properties.toMap();
	}

	/**
	 * <p>
	 * Sets a project-specific property.
	 * </p>
	 *
	 * @param key
	 * @param value
	 * @return <code>true</code> if and only if the property was changed
	 */
	@Override
	public boolean setProperty(String key, String value) {
		String oldValue = getProperty(key);
		if (Util.equals(oldValue, value)) {
			return false;
		}

		initProperty(key, value);

		updateProjectSpec();
		firePropertyChange(new PropertyChangeEvent(this, key, oldValue, value));

		return true;
	}

	/**
	 * <p>
	 * Returns the list of vendors to use for this project, using the device
	 * filter for this project.
	 * </p>
	 * <p>
	 * To get a list of all vendors, use MoSyncTool.getVendors.
	 * </p>
	 *
	 * @return
	 */
	public IVendor[] getFilteredVendors() {
		IVendor[] allVendors = getProfileManager().getVendors();
		return AbstractDeviceFilter.filterVendors(allVendors, deviceFilter);
	}

	public ProfileManager getProfileManager() {
		return MoSyncTool.getDefault().getProfileManager(
				getProfileManagerType());
	}

	public int getProfileManagerType() {
		int mgrType = PropertyUtil.getInteger(this, PROFILE_MANAGER_TYPE_KEY,
				MoSyncTool.LEGACY_PROFILE_TYPE);
		return mgrType;
	}

	/**
	 * Sets the profile manager type of this project, and if it
	 * is different from it's current type: makes
	 * sure to change the target profile to a default one as well
	 * as setting all filters to a reasonable value. (This is actually more of a conversion
	 * method than a setter).
	 * @param type
	 */
	public void setProfileManagerType(int type) {
		setProfileManagerType(type, false);
	}

	public void setProfileManagerType(int type, boolean force) {
		if (force || getProfileManagerType() != type) {
			PropertyUtil.setInteger(this, MoSyncProject.PROFILE_MANAGER_TYPE_KEY, type);
			getDeviceFilter().removeAllFilters();
			addCapabilityFilters(this, createCapabilitiesFromPermissions());
			setTargetProfile(null);
		}
	}

	private String[] createCapabilitiesFromPermissions() {
		IApplicationPermissions permissions = getPermissions();
		List<String> permissionNames = permissions.getRequestedPermissions(true);
		TreeSet<String> availableCapabilities = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		availableCapabilities.addAll(
				Arrays.asList(ProfileDBManager.getInstance().getAvailableCapabilities(true)));
		ArrayList<String> result = new ArrayList<String>();
		for (String permission : permissionNames) {
			// We only support one-level permissions as of now
			if (availableCapabilities.contains(permission) && permission.indexOf('/') == -1) {
				result.add(permission);
			}
		}
		return result.toArray(new String[0]);
	}

	/**
	 * <p>
	 * Returns the list of profiles to use for this project, using the device
	 * filter associated with this project.
	 * </p>
	 * <p>
	 * To get a list of all profiles, use MoSyncTool.getProfiles.
	 * </p>
	 *
	 * @return
	 */
	public IProfile[] getFilteredProfiles() {
		IProfile[] profiles = getProfileManager().getProfiles(deviceFilter);
		return profiles;
	}

	public IProfile[] getFilteredProfiles(IVendor vendor) {
		return ProfileManager.filterProfiles(vendor.getProfiles(), deviceFilter);
	}

	/**
	 * Returns the default value of a project specific property
	 */
	@Override
	public String getDefaultProperty(String key) {
		return CoreMoSyncPlugin.getDefault().getDefaultValue(this, key);
	}

	@Override
	public boolean isDefault(String key) {
		return properties.get(key) == null;
	}

	/**
	 * <p>
	 * Sets the property, regardless of any previous value.
	 * </p>
	 * <p>
	 * Any property set by this method will be stored in the SHARED_PROPERTY
	 * properties.
	 * </p>
	 *
	 * @param key
	 * @param value
	 *            If <code>null</code>, then the entry is removed
	 */
	@Override
	public void initProperty(String key, String value) {
		initProperty(key, value, getStoreForKey(key), false);
	}

	private int getStoreForKey(String key) {
		// As of this moment, only workspace local or shared...
		return key.endsWith(SecureProperties.DEFAULT_SECURE_PROPERTY_SUFFIX) ? WORKSPACE_LOCAL_PROPERTY
				: SHARED_PROPERTY;
	}

	/**
	 * <p>
	 * Sets the property, regardless of any previous value.
	 * </p>
	 *
	 * @param key
	 * @param value
	 *            If <code>null</code>, then the entry is removed
	 * @param store
	 *            The store that this key should be set in, ie LOCAL, SHARED or
	 *            WORKSPACE_LOCAL
	 */
	public void initProperty(String key, String value, int store, boolean save) {
		if (value == null) {
			getProperties(store).remove(key);
		} else {
			getProperties(store).put(key, value);
		}
		if (save) {
			updateProjectSpec(store);
		}
	}

	private Map<String, String> getProperties(int store) {
		switch (store) {
		case SHARED_PROPERTY:
			return sharedProperties;
		case LOCAL_PROPERTY:
			return localProperties;
		case WORKSPACE_LOCAL_PROPERTY:
			return workspaceLocalProperties;
		default:
			throw new IllegalArgumentException();
		}
	}

	/**
	 * A convenience method for returning the exclusion filter for a
	 * project/config.
	 *
	 * @param project
	 *            The project for which to find the filter
	 * @param withStandardExcludes
	 *            Whether to use the standard excludes of the project when
	 *            computing the filter
	 * @return
	 */
	public static PathExclusionFilter getExclusionFilter(MoSyncProject project,
			boolean withStandardExcludes) {
		if (project == null) {
			return null;
		}

		IPropertyOwner properties = project.getPropertyOwner();
		PathExclusionFilter result = withStandardExcludes ? project.excludes
				.get(properties) : null;
		if (result == null) {
			// TODO: Efficient - well, not really... And this method is heavily
			// used!
			String[] standardExclusion = withStandardExcludes ? PropertyUtil
					.getStrings(properties, STANDARD_EXCLUDES_FILTER_KEY)
					: new String[0];
			String[] exclusions = PropertyUtil.getStrings(properties,
					EXCLUDE_FILTER_KEY);
			String[] aggregateExclusions = new String[standardExclusion.length
					+ exclusions.length];
			System.arraycopy(standardExclusion, 0, aggregateExclusions, 0,
					standardExclusion.length);
			System.arraycopy(exclusions, 0, aggregateExclusions,
					standardExclusion.length, exclusions.length);
			Map<String, String> params = properties.getProperties();

			ArrayList<String> finalExclusions = new ArrayList<String>();
			// TODO: All params should be able to have % tags.
			for (int i = 0; i < aggregateExclusions.length; i++) {
				String excluded = Util.replace(aggregateExclusions[i], params);
				IPath[] excludedPaths = PropertyUtil.toPaths(excluded);
				String[] excludedPathsStr = new String[excludedPaths.length];
				for (int j = 0; j < excludedPathsStr.length; j++) {
					excludedPathsStr[j] = excludedPaths[j].toPortableString();
				}

				finalExclusions.addAll(Arrays.asList(excludedPathsStr));
			}

			result = PathExclusionFilter.parse(finalExclusions
					.toArray(new String[0]));
			if (withStandardExcludes) {
				project.excludes.put(properties, result);
			}
		}

		return result;
	}

	public static void setExclusionFilter(MoSyncProject project,
			PathExclusionFilter filter) {
		PropertyUtil.setStrings(project.getPropertyOwner(), EXCLUDE_FILTER_KEY,
				filter.getFileSpecs());
	}

	@Override
	public String getContext() {
		return CONTEXT;
	}

	/**
	 * Returns the name of this MoSync project.
	 *
	 * @return
	 */
	public String getName() {
		return getWrappedProject().getName();
	}

	/**
	 * <p>
	 * Adds all properties of <code>properties</code> to this project.
	 * </p>
	 * <p>
	 * They will all be added to the SHARED store.
	 * </p>
	 *
	 * @param properties
	 */
	public void setProperties(Map<String, String> properties) {
		sharedProperties.putAll(properties);
		updateProjectSpec();
		invalidatePropertyDependentObjects();
	}

	/**
	 * <p>
	 * Returns the build state for a variant manager of this project. All
	 * non-finalizer build states are cached.
	 * </p>
	 *
	 * @return
	 */
	public IBuildState getBuildState(IBuildVariant variant) {
		IBuildState result = cachedBuildStates.get(variant);
		boolean wasNull = true; // result == null;
		if (wasNull) {
			result = new BuildState(this, variant);
		}

		if (wasNull) {
			cachedBuildStates.put(variant, result);
		}

		return result;
	}

	/**
	 * Returns the current build configuration. If none is assigned, this method
	 * will try to assign a default build configuration (
	 * <code>IBuildConfiguration.RELEASE_ID</code>). If the default build
	 * configuration does not exist, <code>null</code> will be returned. This
	 * method will return a value regardless of what
	 * <code>isBuildConfigurationsSupported</code> returns.
	 *
	 * @return
	 */
	public IBuildConfiguration getActiveBuildConfiguration() {
		if (currentBuildConfig == null) {
			currentBuildConfig = getBuildConfiguration(IBuildConfiguration.RELEASE_ID);
		}

		return currentBuildConfig;
	}

	/**
	 * Sets the current build configuration. A property change event with event
	 * type <code>BUILD_CONFIGURATION_CHANGED</code> is triggered.
	 *
	 * @param id
	 *            The new build configuration to use
	 * @throws IllegalArgumentException
	 *             If no build configuration with the given id exists.
	 */
	public void setActiveBuildConfiguration(String id) {
		IBuildConfiguration oldCfg = getActiveBuildConfiguration();
		Object oldId = oldCfg == null ? null : oldCfg.getId();
		IBuildConfiguration newConfiguration = getBuildConfiguration(id);
		if (newConfiguration == null) {
			throw new IllegalArgumentException(MessageFormat.format(
					"No configuration with id {0}", id));
		}
		if (!id.equals(oldId)) {
			currentBuildConfig = newConfiguration;
			updateProjectSpec();
			firePropertyChange(new PropertyChangeEvent(this,
					BUILD_CONFIGURATION_CHANGED, oldId, id));
		}
	}

	public Set<String> getBuildConfigurations() {
		return new TreeSet<String>(configurations.keySet());
	}

	public Set<IBuildConfiguration> getBuildConfigurations(
			Collection<String> ids) {
		TreeSet<IBuildConfiguration> configs = new TreeSet<IBuildConfiguration>(
				BuildConfiguration.DEFAULT_COMPARATOR);
		for (String id : ids) {
			configs.add(getBuildConfiguration(id));
		}

		return configs;
	}

	/**
	 * Returns the ids of all build configurations that has a set of types
	 *
	 * @param types
	 *            The types to match against
	 * @return The build configurations that have <b>all</b> the specified
	 *         types, sorted in case-insensitive alphabetical order.
	 */
	public SortedSet<String> getBuildConfigurationsOfType(String... types) {
		TreeSet<String> result = new TreeSet<String>(
				String.CASE_INSENSITIVE_ORDER);
		for (IBuildConfiguration cfg : configurations.values()) {
			boolean doAdd = true;
			for (int i = 0; i < types.length; i++) {
				doAdd &= cfg.getTypes().contains(types[i]);
			}

			if (doAdd) {
				result.add(cfg.getId());
			}
		}

		return result;
	}

	/**
	 * Returns the build configuration for a given id
	 *
	 * @param id
	 * @return <code>null</code> if the given id is <code>null</code> or if
	 *         there is no build configuration with that id
	 */
	public IBuildConfiguration getBuildConfiguration(String id) {
		if (id == null) {
			return null;
		}
		return configurations.get(id);
	}

	public IBuildConfiguration installBuildConfiguration(String id,
			String[] types) {
		BuildConfiguration newConfig = new BuildConfiguration(this, id, types);
		installBuildConfiguration(newConfig);
		return newConfig;
	}

	public void installBuildConfiguration(IBuildConfiguration newConfig) {
		String id = newConfig.getId();
		this.configurations.put(id, newConfig);
		updateProjectSpec();
		firePropertyChange(new PropertyChangeEvent(this,
				BUILD_CONFIGURATION_CHANGED, null, newConfig));
	}

	public void deinstallBuildConfiguration(String id) {
		IBuildConfiguration removed = this.configurations.remove(id);
		removed.getProperties().clear();
		if (currentBuildConfig == removed) {
			currentBuildConfig = null;
			if (!configurations.isEmpty()) {
				Entry<String, IBuildConfiguration> entry = configurations
						.lowerEntry(id);
				currentBuildConfig = entry == null ? configurations
						.firstEntry().getValue() : entry.getValue();
			}
		}
		updateProjectSpec();
		listeners
				.firePropertyChange(BUILD_CONFIGURATION_CHANGED, removed, null);
	}

	public boolean setBuildConfigurationsSupported(
			boolean isBuildConfigurationsSupported) {
		if (this.isBuildConfigurationsSupported != isBuildConfigurationsSupported) {
			this.isBuildConfigurationsSupported = isBuildConfigurationsSupported;
			updateProjectSpec();
			listeners.firePropertyChange(BUILD_CONFIGURATION_SUPPORT_CHANGED,
					!isBuildConfigurationsSupported,
					isBuildConfigurationsSupported);
			return true;
		}
		return false;
	}

	public boolean areBuildConfigurationsSupported() {
		return isBuildConfigurationsSupported;
	}

	/**
	 * Activates build configurations for this project. If there already are
	 * installed build configurations, this amounts to calling
	 * <code>setBuildConfigurationsSupported(true);</code>, otherwise a default
	 * set of build configurations are installed.
	 */
	public void activateBuildConfigurations() {
		setBuildConfigurationsSupported(true);

		if (configurations.isEmpty()) {
			configurations.put("Release", new BuildConfiguration(this,
					IBuildConfiguration.RELEASE_ID,
					IBuildConfiguration.RELEASE_TYPE));
			configurations.put("Debug", new BuildConfiguration(this,
					IBuildConfiguration.DEBUG_ID,
					IBuildConfiguration.DEBUG_TYPE));
			setActiveBuildConfiguration("Release");
		}
	}

	public IPropertyOwner getPropertyOwner() {
		if (isBuildConfigurationsSupported && currentBuildConfig != null) {
			return currentBuildConfig.getProperties();
		}

		return this;
	}

	/**
	 * Returns the secure property owner of this project.
	 *
	 * @return
	 */
	public ISecurePropertyOwner getSecurePropertyOwner() {
		return securePropertyOwner;
	}

	public LibraryLookup getLibraryLookup(IBuildVariant variant, IPropertyOwner buildProperties) {
		// TODO: cache?
		return new LibraryLookup(MoSyncBuilder.getLibraryPaths(
				getWrappedProject(), buildProperties),
				MoSyncBuilder.getLibraries(this, variant, buildProperties));
	}

	public IApplicationPermissions getPermissions() {
		return permissions;
	}

	/**
	 * Returns the icon file associated with this project.
	 *
	 * @return the icon file associated with this project, null if no icon file
	 *         exists.
	 */
	public File getIconFile() {
		return findIconFile(getWrappedProject().getLocation().toFile());
	}

	/**
	 * Recursive search for a file that ends with
	 * DefaultPackager.ICON_FILE_EXTENSION.
	 *
	 * @param rootFile
	 *            The root directory to begin searching in.
	 * @return a file on success and null if no such file was found.
	 */
	private File findIconFile(final File rootFile) {
		if (rootFile.isDirectory()) {
			final File[] childs = rootFile.listFiles();
			for (File child : childs) {
				File iconFile = findIconFile(child);
				if (iconFile != null) {
					IFile[] iconResource = getWrappedProject().getWorkspace()
							.getRoot()
							.findFilesForLocationURI(iconFile.toURI());
					if (iconResource.length > 0
							&& getExclusionFilter(this, true).accept(
									iconResource[0])) {
						return iconFile;
					}
				}
			}
			return null;
		}

		if (rootFile.getName().endsWith(ICON_FILE_EXTENSION) == true) {
			return rootFile;
		}
		return null;
	}

	/**
	 * Returns the version of the format used to persist the project meta data.
	 *
	 * @return
	 */
	public Version getFormatVersion() {
		return formatVersion;
	}

	public void setFormatVersion(Version formatVersion) {
		this.formatVersion = formatVersion;
		updateProjectSpec();
	}

	/**
	 * Returns a list of the names of all open projects that are compatible
	 * {@link MoSyncProject}s
	 *
	 * @see {@link MoSyncNature#isCompatible(IProject)}
	 * @return
	 * @throws CoreException
	 */
	public static List<String> listAllProjects() {
		ArrayList<String> result = new ArrayList<String>();
		IProject[] allProjects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		for (IProject project : allProjects) {
			try {
				if (project.isOpen() && MoSyncNature.isCompatible(project)) {
					result.add(project.getName());
				}
			} catch (CoreException e) {
				// Should only happen if project is not open
				// and we already check for this - hence this is
				// a runtime exception
				throw new RuntimeException(e);
			}
		}

		return result;
	}

	public String getOutputType() {
		return getProperty(MoSyncBuilder.OUTPUT_TYPE); 
	}
	
	/**
	 * Sets the (preferred) output type of this project.
	 * Will throw an exception with a detailed, user-friendly
	 * message if this operation cannot be performed.
	 * Use {@link #forceOutputType(String)} to ignore any
	 * exceptions -- this will cause changes to the project
	 * that will make it possible to build natively but
	 * may also make the project un-buildable. (This is by design)
	 * @param binaryType
	 * @return
	 * @throws IllegalArgumentException
	 */
	public boolean setOutputType(String binaryType) throws IllegalArgumentException {
		ArrayList<String> errors = new ArrayList<String>();
		
		if (MoSyncBuilder.OUTPUT_TYPE_NATIVE_COMPILE.equals(binaryType)) {
			if (getProfileManagerType() != MoSyncTool.DEFAULT_PROFILE_TYPE) {
				errors.add("Native projects must make use of the platform based profile type.");
			}
			if (!isBuildConfigurationsSupported) {
				errors.add("Native projects require build configuration support");
			}
			
			boolean changedIncludes = false;
			for (String cfg : getBuildConfigurations()) {
				Pair<Boolean, List<IPath>> nativePaths = filteredNativePaths(cfg);
				changedIncludes |= nativePaths.first;
			}
			
			if (changedIncludes) {
				errors.add("Native projects only supports include paths relative to the project.");
			}
		}
		
		if (errors.isEmpty()) {
			return forceOutputType(binaryType);
		} else {
			String errorMsg = "  * " + Util.join(errors.toArray(), "\n  * ");
			throw new IllegalArgumentException(errorMsg);
		}
	}
	
	private Pair<Boolean, List<IPath>> filteredNativePaths(String cfg) {
		return MoSyncBuilder.filterNativeIncludePaths(this, new BuildVariant(getTargetProfile(), cfg));
	}
	
	public boolean forceOutputType(String binaryType) {
		boolean result = false;
		if (MoSyncBuilder.OUTPUT_TYPE_NATIVE_COMPILE.equals(binaryType)) {
			result = setBuildConfigurationsSupported(true);
			setProfileManagerType(MoSyncTool.DEFAULT_PROFILE_TYPE);
			for (String cfg : getBuildConfigurations()) {
				Pair<Boolean, List<IPath>> nativePaths = filteredNativePaths(cfg);
				PropertyUtil.setPaths(getBuildConfiguration(cfg).getProperties(),
						MoSyncBuilder.ADDITIONAL_NATIVE_INCLUDE_PATHS, nativePaths.second.toArray(new IPath[0]));
			}
		}
		result |= setProperty(MoSyncBuilder.OUTPUT_TYPE, binaryType);
		return result;
	}


}
