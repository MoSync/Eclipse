package com.mobilesorcery.sdk.testing.project;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.mobilesorcery.sdk.testing.TestPlugin;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;

public class MoSyncProjectTestManager {

	private static final String TEST_PREFIX = "test.ns:";
	private static final String IS_TEST_PROJECT = TEST_PREFIX + "is.testproject";
	public static final String TEST_RESOURCES = TEST_PREFIX + "test.res";

    private static final String TEST_LIBRARY = "testify.lib";
    private static final String TEST_LIBRARY_DEBUG = "testifyD.lib";
	
	private MoSyncProject project;
	private Set<IPath> testResources = new HashSet<IPath>();

	public MoSyncProjectTestManager(MoSyncProject project) {
		this.project = project;
		init();
	}
	
	private void init() {
	    testResources = new HashSet<IPath>(Arrays.asList(PropertyUtil.getPaths(project, TEST_RESOURCES)));
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
	    // TODO: We now use a specific property page to modify this; and this
	    // property page is specific to the testing plugin. So we kind of assume
	    // that the only plugin that will ever modify this property is the testing
	    // plugin. Kind of ugly, so FIXME. Maybe even time to implement path containers...
	    project.setProperty(MoSyncProject.STANDARD_EXCLUDES_FILTER_KEY, "%" + TEST_RESOURCES + "%");
        
	    configureBuildConfiguration(true);
	    configureBuildConfiguration(false);
	}

	private void configureBuildConfiguration(boolean isDebug) {
        IBuildConfiguration testCfg = ensureHasTestConfigs(isDebug);
        
        // Now we just brutally set the standard excludes to be the test resources
        testCfg.getProperties().setProperty(MoSyncProject.STANDARD_EXCLUDES_FILTER_KEY, "%EMPTY%");
        PropertyUtil.setPaths(project, TEST_RESOURCES, testResources.toArray(new IPath[0]));
        
        addRequiredLibraries(testCfg.getProperties(), isDebug);
        setMinimumMemorySizes(testCfg);
    }

    private void setMinimumMemorySizes(IBuildConfiguration testCfg) {
	    // The framework itself requires a certain amount of memory.
        setMinimumMemorySize(testCfg.getProperties(), MoSyncBuilder.MEMORY_HEAPSIZE_KB, 128);
        setMinimumMemorySize(testCfg.getProperties(), MoSyncBuilder.MEMORY_STACKSIZE_KB, 64);
        setMinimumMemorySize(testCfg.getProperties(), MoSyncBuilder.MEMORY_DATASIZE_KB, 256);
	    
	}
	
	private void setMinimumMemorySize(NameSpacePropertyOwner properties, String key, int minMemSizeInKb) {
	    Integer currentMemSize = PropertyUtil.getInteger(properties, key);
	    if (currentMemSize == null || currentMemSize < minMemSizeInKb) {
	        PropertyUtil.setInteger(properties, key, minMemSizeInKb);
        }
    }

    private void addRequiredLibraries(NameSpacePropertyOwner properties, boolean isDebug) {
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

    public List<IBuildConfiguration> getTestConfigs(boolean isDebug) {
        return new ArrayList<IBuildConfiguration>(project.getBuildConfigurations(project.getBuildConfigurationsOfType(getTypes(isDebug))));
	}
	
	private IBuildConfiguration ensureHasTestConfigs(boolean isDebug) {
		project.activateBuildConfigurations();
		List<IBuildConfiguration> cfgs = getTestConfigs(isDebug);
		if (!cfgs.isEmpty()) {
		    return cfgs.get(0);
		}
		
		String cfgName = isDebug ? "Test_Debug" : "Test";
		String testCfgId = BuildConfiguration.createUniqueId(project, cfgName);
		
		// We try to create the test configuration to be as close as possible to existing configurations.
		// TODO: If more than 1, use wizard to let user select which cfg to base it on?
		List<String> prototypes = new ArrayList<String>(project.getBuildConfigurationsOfType(isDebug ? IBuildConfiguration.DEBUG_TYPE : IBuildConfiguration.RELEASE_TYPE));
		
		String[] types = getTypes(isDebug);
		IBuildConfiguration testCfg = null;
		if (prototypes.isEmpty()) {
		    testCfg = project.installBuildConfiguration(testCfgId, types);
		} else {
		    testCfg = project.getBuildConfiguration(prototypes.get(0)).clone(testCfgId);
		    testCfg.setTypes(Arrays.asList(types));
		    project.installBuildConfiguration(testCfg);
		}
		return testCfg;
	}
	
	private String[] getTypes(boolean isDebug) {
        String baseCfgId = isDebug ? IBuildConfiguration.DEBUG_ID : IBuildConfiguration.RELEASE_ID;
        String[] types = new String[] { baseCfgId , TestPlugin.TEST_BUILD_CONFIGURATION_TYPE };
        return types; 
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
		return testResources.contains(resource.getProjectRelativePath());
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
