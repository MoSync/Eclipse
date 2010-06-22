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

package com.mobilesorcery.sdk.ui;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.profiles.ICompositeDeviceFilter;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.IVendor;
import com.mobilesorcery.sdk.profiles.filter.CompositeDeviceFilter;
import com.mobilesorcery.sdk.profiles.filter.ConstantFilter;
import com.mobilesorcery.sdk.profiles.filter.FeatureFilter;
import com.mobilesorcery.sdk.profiles.filter.ProfileFilter;
import com.mobilesorcery.sdk.profiles.filter.ConstantFilter.RelationalOp;

/**
 * 
 * @author Mattias Bybro
 * @deprecated Modify this to ignore .mopro files, until then it's deprecated.
 */
public class ImportProjectsRunnable extends WorkspaceModifyOperation {

	public final static int COPY_ALL_FILES = 0;
	public final static int COPY_ONLY_FILES_IN_PROJECT_DESC = 1;
	public final static int DO_NOT_COPY = 2;
	
	/**
	 * A constant indicating we should use the .mosyncproject file (if it exists)
	 * rather than the legacy .msp/.mopro file format.
	 */
	public final static int USE_NEW_PROJECT_IF_AVAILABLE = 1 << 12;

	static FileFilter COPY_FILE_FILTER = new FileFilter() {
		public boolean accept(File file) {
			String name = file.getName();
			return !name.equals(".project") && !name.equals(".cproject") && !name.equals(MoSyncProject.MOSYNC_PROJECT_META_DATA_FILENAME); //$NON-NLS-1$ //$NON-NLS-2$ 
		}
	};

	private File[] projectDescriptions;
    private String[] preferredProjectNames;
	private Map<String, String> keyMap;
	private int copyStrategy;
	private boolean useNewProjectIfAvailable;
	private List<IProject> result;
    private boolean showDialogOnSuccess = false;

    public ImportProjectsRunnable(File[] projectDescriptions, int strategy) {
        this(projectDescriptions, null, strategy, null);
    }
    
    public ImportProjectsRunnable(File[] projectDescriptions, String[] preferredProjectNames, int strategy) {
        this(projectDescriptions, preferredProjectNames, strategy, null);
    }
    
    public ImportProjectsRunnable(File[] projectDescriptions, int strategy, List<IProject> result) {
        this(projectDescriptions, null, strategy, result);
    }
        
    public ImportProjectsRunnable(File[] projectDescriptions, String[] preferredProjectNames, int strategy, List<IProject> result) {
    	this.projectDescriptions = projectDescriptions;
    	this.preferredProjectNames = preferredProjectNames;
		this.copyStrategy = strategy & 0x3; // Max value of copy strategies.
		this.useNewProjectIfAvailable = (strategy & USE_NEW_PROJECT_IF_AVAILABLE) == USE_NEW_PROJECT_IF_AVAILABLE;
		this.result = result == null ? new ArrayList<IProject>() : result;
	}

	public Job createJob(boolean schedule) {
	    Job importJob = new Job("Import projects") {
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    execute(monitor);
                    if (showDialogOnSuccess) {
                       showImportSuccessDialog(result.size()); 
                    }
                    return Status.OK_STATUS;
                } catch (CoreException e) {
                    return e.getStatus();
                } 
            }	        
	    };
	    
	    importJob.setUser(true);
	    importJob.setRule(ResourcesPlugin.getWorkspace().getRoot());
	    if (schedule) {
	        importJob.schedule();
	    }
	    return importJob;
	}

    private void showImportSuccessDialog(final int newProjectCount) {
        if (newProjectCount > 0) {
            final Display d = PlatformUI.getWorkbench().getDisplay();
            d.syncExec(new Runnable() {
                public void run() {
                    Shell shell = new Shell(d);
                    MessageDialog.openInformation(new Shell(d),
                            "Imported Example Projects",
                            MessageFormat.format("Imported {0} example projects", newProjectCount));
                    shell.dispose();
                }
            });
        }
    }
    
	protected void execute(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(Messages.ImportProjectsRunnable_ImportProgress,
				projectDescriptions.length);

		ArrayList<IStatus> errorStatus = new ArrayList<IStatus>();

		for (int i = 0; i < projectDescriptions.length; i++) {
			if (monitor.isCanceled()) {
				return;
			}
			try {
				importProject(monitor, getPreferredProjectName(i), projectDescriptions[i]);
			} catch (Exception e) {
				errorStatus.add(new Status(IStatus.ERROR, MosyncUIPlugin.PLUGIN_ID,
						projectDescriptions[i] + "; " + e.getMessage(), e));
			}
		}

		if (!errorStatus.isEmpty()) {
			MultiStatus compoundError = new MultiStatus(MosyncUIPlugin.PLUGIN_ID,
					IStatus.ERROR, errorStatus.toArray(new IStatus[0]),
					Messages.ImportProjectsRunnable_SomeProjectsFailed, null);
			CoreException ex = new CoreException(compoundError);
			CoreMoSyncPlugin.getDefault().log(ex);
			throw ex;
		}
	}

	private String getPreferredProjectName(int ix) {
	    if (preferredProjectNames == null || ix < 0 || ix >= preferredProjectNames.length) {
	        return null;
	    } else {
	        return preferredProjectNames[ix];
	    }
    }

    private void importProject(IProgressMonitor monitor, String preferredProjectName, File projectDescription)
			throws CoreException {
		if (projectDescription.isDirectory()) {
			throw new CoreException(new Status(IStatus.ERROR, MosyncUIPlugin.PLUGIN_ID, "Project description must not be a directory"));
		}
		
		/*if (shouldUseNewFormatIfAvailable() && copyStrategy != COPY_ALL_FILES) {
			throw new CoreException(new Status(IStatus.ERROR, MosyncUIPlugin.PLUGIN_ID, "\'Use new format if available\' can only be used with copy all files"));
		}*/
		
		monitor = new SubProgressMonitor(monitor, 4);
		String projectName = preferredProjectName == null ? Util.getNameWithoutExtension(projectDescription) : preferredProjectName;

		IPath projectMetaDataLocation = getProjectMetaDataLocation(projectDescription);

		URI location = shouldCopy() ? null : projectDescription.getParentFile()
				.toURI();

		IProject project = createProjectWithUniqueName(projectName, location,
				monitor);

		project.open(new SubProgressMonitor(monitor, 1));

		MoSyncProject mosyncProject = MoSyncProject.create(project);

		if (!shouldUseNewFormatIfAvailable() || projectMetaDataLocation == null) {
			parseProjectDescription(new SubProgressMonitor(monitor, 1), mosyncProject, projectDescription);
		} else if (shouldCopy()) {
			copyFilesToProject(monitor, mosyncProject, projectDescription, new String[] { projectMetaDataLocation.toFile().getParent() });
		}

		// And we will have to re-initialize in case there is a .mosyncproject file
		// available.
		MoSyncProject.create(project,
				projectMetaDataLocation);

		project.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(
				monitor, 1));
		
		if (result != null) {
			result.add(project);
		}
	}

	private IPath getProjectMetaDataLocation(File projectDescription) {
		File projectMetaDataFile = new File(projectDescription.getParentFile(),
				MoSyncProject.MOSYNC_PROJECT_META_DATA_FILENAME);
		return projectMetaDataFile.exists() && shouldUseNewFormatIfAvailable() ? new Path(
				projectMetaDataFile.getAbsolutePath())
				: null;
	}

	public boolean shouldUseNewFormatIfAvailable() {
		return useNewProjectIfAvailable;
	}

	public IProject createProjectWithUniqueName(String projectName, URI location,
			IProgressMonitor monitor) throws CoreException {
		String newProjectName = projectName;
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IProject result = workspaceRoot.getProject(projectName);
		int i = 1;

		boolean exists = true;
		while (exists) {
			newProjectName = i == 1 ? projectName : (projectName + i);
			result = workspaceRoot.getProject(newProjectName);
			exists = result.exists();
			if (i > 100) {
				throw new CoreException(
						new Status(
								IStatus.ERROR,
								MosyncUIPlugin.PLUGIN_ID,
								Messages.ImportProjectsRunnable_UniqueNameCreationFailed));
			}

			// Since IResource.exists() is case-sensitive whereas the underlying
			// OS may
			// not be, we must create the project here and check for
			// IResourceStatus.CASE_VARIANT_EXISTS.

			try {
				if (!exists) {
					MosyncUIPlugin.createProject(result, location, monitor);
					exists = false;
				}
			} catch (CoreException e) {
				exists = true; // So we are stuck in the loop.
				if (e.getStatus().getCode() != IResourceStatus.CASE_VARIANT_EXISTS) {
					throw e;
				} // else iterate again.
			}

			// Increment, please.
			i++;
		}

		return result;
	}

	private void parseProjectDescription(IProgressMonitor monitor,
			MoSyncProject project, File projectDescription)
			throws CoreException {
		try {
			boolean success = true;

			DocumentBuilder builder = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			Document doc = builder.parse(projectDescription);
			String[] projectPaths = findFiles(doc, projectDescription);

			if (shouldCopy()) {
				success &= copyFilesToProject(monitor, project,
						projectDescription, projectPaths);
			}

			Map<String, String> properties = parseProperties(doc);

			String targetVendor = properties.remove("targetVendor"); //$NON-NLS-1$
			String targetDevice = properties.remove("targetDevice"); //$NON-NLS-1$

			if (targetVendor != null && targetDevice != null) {
				IVendor vendor = MoSyncTool.getDefault()
						.getVendor(targetVendor);
				if (vendor != null) {
					IProfile profile = vendor.getProfile(targetDevice);
					if (profile != null) {
						project.initTargetProfile(profile);
					}
				}
			}

			project.setProperties(properties);

			ICompositeDeviceFilter filter = parseCriteria(doc);
			project.setDeviceFilter(filter);

			if (!success) {
				throw new IOException(MessageFormat.format(
						Messages.ImportProjectsRunnable_ImportFailed, project
								.getName()));
			}

		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR,
					MosyncUIPlugin.PLUGIN_ID, e.getMessage(), e));
		}
	}

	public boolean shouldCopy() {
		return copyStrategy != DO_NOT_COPY;
	}

	private boolean copyFilesToProject(IProgressMonitor monitor,
			MoSyncProject project, File projectDescription,
			String[] projectPaths) throws CoreException {
		boolean success = true;

		File projectDir = projectDescription.getParentFile();
		if (copyStrategy == COPY_ALL_FILES) {
			try {
				Util.copyDir(new SubProgressMonitor(monitor, 1), projectDir,
						project.getWrappedProject().getLocation().toFile(),
						COPY_FILE_FILTER);
			} catch (IOException e) {
				e.printStackTrace();
				success = false;
			}
		} else {
			for (int i = 0; i < projectPaths.length; i++) {
				File copySrc = new File(projectDir, projectPaths[i]);

				// If project has files that are not located in any subfolder of
				// the
				// project's;
				// create link instead
				if (projectPaths[i].contains("..")) { //$NON-NLS-1$
					String filename = copySrc.getName();
					IFile projectFile = project.getWrappedProject().getFile(
							new Path(filename));
					projectFile.createLink(new Path(copySrc.getAbsolutePath()),
							IFile.ALLOW_MISSING_LOCAL, new SubProgressMonitor(
									monitor, 1));
				} else if (copyStrategy == COPY_ONLY_FILES_IN_PROJECT_DESC) {
					Path path = new Path(projectPaths[i]);
					IFile projectFile = project.getWrappedProject().getFile(
							path);
					File copyDest = projectFile.getLocation().toFile();
					try {
						Util.copy(new SubProgressMonitor(monitor, 1),
								copySrc, copyDest, COPY_FILE_FILTER);
					} catch (IOException e) {
						e.printStackTrace();
						success = false;
					}
				}
			}
		}

		return success;
	}

	private Map<String, String> parseProperties(Document doc) {
		Map<String, String> result = new HashMap<String, String>();

		NodeList propertiesList = doc
				.getElementsByTagName(Messages.ImportProjectsRunnable_10);
		for (int i = 0; i < propertiesList.getLength(); i++) {
			Element propertyElement = (Element) propertiesList.item(i);
			String name = propertyElement.getAttribute("name"); //$NON-NLS-1$
			String value = propertyElement.getAttribute("value"); //$NON-NLS-1$
			if (hasValue(name) && hasValue(value)) {
				result.put(mapKey(name), mapValue(name, value));
			}
		}
		return result;
	}

	private String mapKey(String key) {
		if (keyMap == null) {
			keyMap = initKeyMap();
		}

		String newKey = keyMap.get(key);
		return newKey == null ? key : newKey;
	}

	private String mapValue(String name, String value) {
		if ("additionalIncludeDirectories".equals(name) || "additionalLibraryDirectories".equals(name) //$NON-NLS-1$ //$NON-NLS-2$
				|| "additionalDependencies".equals(name)) { //$NON-NLS-1$
			// Instead of space as a path separator, we'll use a comma.
			value = value.replace(' ', ',');
		}

		return value;
	}

	private Map<String, String> initKeyMap() {
		Map<String, String> result = new HashMap<String, String>();

		// Builder
		result
				.put(
						"ignoreDefaultIncludeDirectories", MoSyncBuilder.IGNORE_DEFAULT_INCLUDE_PATHS); //$NON-NLS-1$
		result
				.put(
						"ignoreDefaultLibraryDirectories", MoSyncBuilder.IGNORE_DEFAULT_LIBRARY_PATHS); //$NON-NLS-1$
		result
				.put(
						"ignoreDefaultLibraries", MoSyncBuilder.IGNORE_DEFAULT_LIBRARIES); //$NON-NLS-1$

		result
				.put(
						"additionalIncludeDirectories", MoSyncBuilder.ADDITIONAL_INCLUDE_PATHS); //$NON-NLS-1$
		result
				.put(
						"additionalLibraryDirectories", MoSyncBuilder.ADDITIONAL_LIBRARY_PATHS); //$NON-NLS-1$
		result
				.put(
						"additionalDependencies", MoSyncBuilder.ADDITIONAL_LIBRARIES); //$NON-NLS-1$

		result.put("extraCmd", MoSyncBuilder.EXTRA_COMPILER_SWITCHES); //$NON-NLS-1$
		result.put("deadCodeElim", MoSyncBuilder.DEAD_CODE_ELIMINATION); //$NON-NLS-1$

		result.put("extraResourcer", MoSyncBuilder.EXTRA_RES_SWITCHES); //$NON-NLS-1$
		result.put("extraLinker", MoSyncBuilder.EXTRA_LINK_SWITCHES); //$NON-NLS-1$

		// Config-type "app"/"lib" are exactly the same in old/new project files
		result.put("configType", MoSyncBuilder.PROJECT_TYPE); //$NON-NLS-1$

		// Symbian
		// NOTE: Changed link time dependency to s60 plugin, since
		// we do not want that dependency. Also, we will kill this
		// class soon (or, at least put it in some legacy plugin), so no problem.
		//result.put("S60v2UID", PropertyInitializer.S60V2_UID); //$NON-NLS-1$
		//result.put("S60v3UID", PropertyInitializer.S60V3_UID); //$NON-NLS-1$
		result.put("S60v2UID", "symbian.uids:s60v2uid"); //$NON-NLS-1$
		result.put("S60v3UID", "symbian.uids:s60v3uid"); //$NON-NLS-1$

		/*
		 * Not yet implemented: <Property name="configType" value="app"/>
		 * <Property name="screenWidth" value="240"/> <Property
		 * name="screenHeight" value="320"/> <Property name="deadCodeElim"
		 * value="false"/> <Property name="extraResourcer" value=""/> <Property
		 * name="extraLinker" value=""/> <Property name="Wall" value=""/>
		 * <Property name="Werror" value=""/> <Property name="Wextra" value=""/>
		 */

		return result;
	}

	private String[] findFiles(Document doc, File projectDescriptorFile) {
		ArrayList<String> result = new ArrayList<String>();
		File projectDescriptorParent = projectDescriptorFile.getParentFile();
		NodeList fileList = doc.getElementsByTagName("File"); //$NON-NLS-1$
		for (int i = 0; i < fileList.getLength(); i++) {
			Element fileElement = (Element) fileList.item(i);
			String relativePath = fileElement.getAttribute("path"); //$NON-NLS-1$
			if (hasValue(relativePath)) {
				result.add(relativePath);
			}
		}

		return result.toArray(new String[result.size()]);
	}

	private ICompositeDeviceFilter parseCriteria(Document doc) {
		CompositeDeviceFilter filter = new CompositeDeviceFilter();

		parseFeatureFilters(doc, filter);
		parseVendorFilters(doc, filter);
		parseProfileFilters(doc, filter);
		parseConstantFilters(doc, filter);

		return filter;
	}

	private void parseConstantFilters(Document doc, CompositeDeviceFilter filter) {
		NodeList constantList = doc.getElementsByTagName("Constant"); //$NON-NLS-1$
		for (int i = 0; i < constantList.getLength(); i++) {
			Element constantElement = (Element) constantList.item(i);
			String opStr = constantElement.getAttribute("op"); //$NON-NLS-1$
			String featureDescription = constantElement.getAttribute("name"); //$NON-NLS-1$
			String thresholdStr = constantElement.getAttribute("value"); //$NON-NLS-1$
			long threshold = Long.MIN_VALUE;
			try {
				if (hasValue(thresholdStr)) {
					threshold = Long.parseLong(thresholdStr);
				}
			} catch (NumberFormatException e) {
				// Ignore.
			}

			RelationalOp op = mapOp(opStr);
			String featureId = hasValue(featureDescription) ? null : MoSyncTool
					.getDefault().getFeatureId(featureDescription);

			if (op != null && featureId != null && threshold > Long.MIN_VALUE) {
				ConstantFilter constantFilter = new ConstantFilter();
				constantFilter.setRelationalOp(op);
				constantFilter.setConstantFeature(featureId);
				constantFilter.setThreshold(threshold);

				filter.addFilter(constantFilter);
			}
		}

	}

	private static RelationalOp mapOp(String op) {
		if ("GT".equals(op)) { //$NON-NLS-1$
			return ConstantFilter.GT;
		} else if ("LT".equals(op)) { //$NON-NLS-1$
			return ConstantFilter.LT;
		} else if ("EQ".equals(op)) { //$NON-NLS-1$
			return ConstantFilter.EQ;
		} else if ("NE".equals(op)) { //$NON-NLS-1$
			return ConstantFilter.NEQ;
		}

		return null;
	}

	private void parseVendorFilters(Document doc, CompositeDeviceFilter filter) {
		NodeList vendorList = doc.getElementsByTagName("Vendor"); //$NON-NLS-1$

		for (int i = 0; i < vendorList.getLength(); i++) {
			Element vendorElement = (Element) vendorList.item(i);
			String requiredVendor = vendorElement.getAttribute("is"); //$NON-NLS-1$
			String disallowedVendor = vendorElement.getAttribute("isNot"); //$NON-NLS-1$
			if (hasValue(requiredVendor)) {
				ProfileFilter requiredVendorFilter = new ProfileFilter();
				requiredVendorFilter.setStyle(ProfileFilter.REQUIRE);
				requiredVendorFilter.setVendor(MoSyncTool.getDefault()
						.getVendor(requiredVendor), true);
				filter.addFilter(requiredVendorFilter);
			}

			if (hasValue(disallowedVendor)) {
				ProfileFilter disallowedVendorFilter = new ProfileFilter();
				disallowedVendorFilter.setStyle(ProfileFilter.DISALLOW);
				disallowedVendorFilter.setVendor(MoSyncTool.getDefault()
						.getVendor(disallowedVendor), true);
				filter.addFilter(disallowedVendorFilter);
			}
		}

	}

	private static boolean hasValue(String str) {
		return str != null && !"".equals(str); //$NON-NLS-1$
	}

	private void parseProfileFilters(Document doc, CompositeDeviceFilter filter) {
		NodeList profileList = doc.getElementsByTagName("Device"); //$NON-NLS-1$
		ProfileFilter requiredProfiles = new ProfileFilter();
		requiredProfiles.setStyle(ProfileFilter.REQUIRE);
		ProfileFilter disallowedProfiles = new ProfileFilter();
		disallowedProfiles.setStyle(ProfileFilter.DISALLOW);

		for (int i = 0; i < profileList.getLength(); i++) {
			Element profileElement = (Element) profileList.item(i);
			String requiredProfile = profileElement.getAttribute("is"); //$NON-NLS-1$
			String disallowedProfile = profileElement.getAttribute("isNot"); //$NON-NLS-1$
			if (hasValue(requiredProfile)) {
				ProfileFilter requiredProfileFilter = new ProfileFilter();
				requiredProfileFilter.setStyle(ProfileFilter.REQUIRE);
				requiredProfileFilter.setProfiles(MoSyncTool.getDefault()
						.getProfiles(requiredProfile), true);
				filter.addFilter(requiredProfileFilter);
			}

			if (hasValue(disallowedProfile)) {
				ProfileFilter disallowedProfileFilter = new ProfileFilter();
				disallowedProfileFilter.setStyle(ProfileFilter.DISALLOW);
				disallowedProfileFilter.setProfiles(MoSyncTool.getDefault()
						.getProfiles(disallowedProfile), true);
				filter.addFilter(disallowedProfileFilter);
			}
		}

	}

	private void parseFeatureFilters(Document doc, CompositeDeviceFilter filter) {
		NodeList requiredFeatureList = doc.getElementsByTagName("Has"); //$NON-NLS-1$

		for (int i = 0; i < requiredFeatureList.getLength(); i++) {
			Element requiredFeatureElement = (Element) requiredFeatureList
					.item(i);
			String featureDescription = requiredFeatureElement
					.getAttribute("feature"); //$NON-NLS-1$
			if (hasValue(featureDescription)) {
				String featureId = MoSyncTool.getDefault().getFeatureId(
						featureDescription);
				if (featureId != null) {
					FeatureFilter requiredFeature = new FeatureFilter();
					requiredFeature.setStyle(FeatureFilter.REQUIRE);
					requiredFeature.addFeatureId(featureId);
					filter.addFilter(requiredFeature);
				}
			}
		}

		NodeList disallowedFeatureList = doc.getElementsByTagName("Lacks"); //$NON-NLS-1$
		for (int i = 0; i < disallowedFeatureList.getLength(); i++) {
			Element disallowedFeatureElement = (Element) disallowedFeatureList
					.item(i);
			String featureDescription = disallowedFeatureElement
					.getAttribute("feature"); //$NON-NLS-1$
			if (hasValue(featureDescription)) {
				String featureId = MoSyncTool.getDefault().getFeatureId(
						featureDescription);
				if (featureId != null) {
					FeatureFilter disallowedFeatures = new FeatureFilter();
					disallowedFeatures.setStyle(FeatureFilter.DISALLOW);
					disallowedFeatures.addFeatureId(featureId);
					filter.addFilter(disallowedFeatures);
				}
			}
		}

	}

    public void setShowDialogOnSuccess(boolean showDialogOnSuccess) {
        this.showDialogOnSuccess = showDialogOnSuccess;
    }

}
