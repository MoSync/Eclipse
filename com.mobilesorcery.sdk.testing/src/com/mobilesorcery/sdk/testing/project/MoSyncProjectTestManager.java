package com.mobilesorcery.sdk.testing.project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.mobilesorcery.sdk.core.BuildConfiguration;
import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.NameSpacePropertyOwner;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;

public class MoSyncProjectTestManager {

	private static final String TEST_PREFIX = "test.ns:";
	private static final String IS_TEST_PROJECT = TEST_PREFIX + "is.testproject";
	public static final String IS_TEST_CONFIG = TEST_PREFIX + "is.test.cfg";
	private static final String TEST_RESOURCES = TEST_PREFIX + "test.res";

    private static final String TEST_LIBRARY = "testify.lib";
    private static final String TEST_LIBRARY_DEBUG = "testifyD.lib";
	
	private MoSyncProject project;
	private Set<IPath> testResources = new HashSet<IPath>();

	public MoSyncProjectTestManager(MoSyncProject project) {
		this.project = project;
	}
	
	/**
	 * Returns whether the project associated with this manager
	 * has been configured to execute tests.
	 * @return
	 */
	public boolean isTestProject() {
		return isTestProject(project);
	}
	
	private static boolean isTestProject(MoSyncProject project) {
		return PropertyUtil.getBoolean(project, IS_TEST_PROJECT);
	}
	
	/**
	 * Configures a project to become a test project
	 */
	public void configureProject() {
		PropertyUtil.setBoolean(project, IS_TEST_PROJECT, true);
		IBuildConfiguration testCfg = ensureHasTestConfigs();
		
		// Now we just brutally set the standard excludes to be the test resources
		project.setProperty(MoSyncProject.STANDARD_EXCLUDES_FILTER_KEY, "%" + TEST_RESOURCES + "%");
		testCfg.getProperties().setProperty(MoSyncProject.STANDARD_EXCLUDES_FILTER_KEY, " ");
		PropertyUtil.setPaths(project, TEST_RESOURCES, testResources.toArray(new IPath[0]));
		
		// TODO: Add support for debug + test cfg
		addRequiredLibraries(false);
		
		setMinimumMemorySizes(testCfg);
	}

	private void setMinimumMemorySizes(IBuildConfiguration testCfg) {
	    // The framework itself requires a certain amount of memory.
        setMinimumMemorySize(testCfg.getProperties(), MoSyncBuilder.MEMORY_HEAPSIZE_KB, 64);
        setMinimumMemorySize(testCfg.getProperties(), MoSyncBuilder.MEMORY_STACKSIZE_KB, 16);
        setMinimumMemorySize(testCfg.getProperties(), MoSyncBuilder.MEMORY_DATASIZE_KB, 80);
	    
	}
	
	private void setMinimumMemorySize(NameSpacePropertyOwner properties, String key, int minMemSizeInKb) {
	    Integer currentMemSize = PropertyUtil.getInteger(properties, key);
	    if (currentMemSize == null || currentMemSize < minMemSizeInKb) {
	        PropertyUtil.setInteger(properties, key, minMemSizeInKb);
        }
    }



    private void addRequiredLibraries(boolean isDebug) {
	    NameSpacePropertyOwner properties = ensureHasTestConfigs().getProperties();
	    IPath[] libs = PropertyUtil.getPaths(properties, MoSyncBuilder.ADDITIONAL_LIBRARIES);
	    boolean hasTestLibrary = false;
	    for (int i = 0; i < libs.length; i++) {
	        hasTestLibrary |= isTestLibrary(libs[i]);
	    }
	    
	    if (!hasTestLibrary) {
	        IPath[] newLibs = new IPath[libs.length + 1];
	        System.arraycopy(libs, 0, newLibs, 0, libs.length);
	        newLibs[libs.length] = new Path(isDebug ? TEST_LIBRARY_DEBUG : TEST_LIBRARY);
	        PropertyUtil.setPaths(properties, MoSyncBuilder.ADDITIONAL_LIBRARIES, newLibs);
	    }
    }

    private boolean isTestLibrary(IPath lib) {
        return TEST_LIBRARY.equalsIgnoreCase(lib.toPortableString()) || TEST_LIBRARY_DEBUG.equalsIgnoreCase(lib.toPortableString());
    }

    public List<IBuildConfiguration> getTestConfigs() {
	    ArrayList<IBuildConfiguration> result = new ArrayList<IBuildConfiguration>();
	    for (String cfgId : project.getBuildConfigurations()) {
            IBuildConfiguration cfg = project.getBuildConfiguration(cfgId);
            if (cfg != null) {
                boolean isTestConfiguration = PropertyUtil.getBoolean(cfg.getProperties(), IS_TEST_CONFIG);
                if (isTestConfiguration) {
                    result.add(cfg);
                }
            }
        }   
	    
	    return result;
	}
	
	public IBuildConfiguration ensureHasTestConfigs() {
		project.activateBuildConfigurations();
		List<IBuildConfiguration> cfgs = getTestConfigs();
		if (!cfgs.isEmpty()) {
		    return cfgs.get(0);
		}
		
		String testCfgId = BuildConfiguration.createUniqueId(project, "Test");
		IBuildConfiguration testCfg = project.installBuildConfiguration(testCfgId);
		PropertyUtil.setBoolean(testCfg.getProperties(), IS_TEST_CONFIG, true);
		return testCfg;
	}
	
	public void assignTestResource(IPath path, boolean assign) {
		if (assign) {
			testResources.add(path);
		} else {
			testResources.remove(path);
		}
		
		configureProject();
	}
	
	public boolean isTestResource(IResource resource) {
		return testResources.contains(resource);
	}
	
	/**
	 * Utility method to check whether a project can 
	 * be configured as a test project
	 * @param project
	 * @return
	 */
	public boolean canConfigureProject(IProject project) {
		MoSyncProject mosyncProject = MoSyncProject.create(project);
		return mosyncProject != null && mosyncProject.areBuildConfigurationsSupported() && !isTestProject(mosyncProject);
	}
 }
