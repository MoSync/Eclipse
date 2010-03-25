package com.mobilesorcery.sdk.testing.project;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.mobilesorcery.sdk.core.BuildConfiguration;
import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PropertyUtil;

public class MoSyncProjectTestManager {

	private static final String TEST_PREFIX = "test.ns:";
	private static final String IS_TEST_PROJECT = TEST_PREFIX + "is.testproject";
	private static final String IS_TEST_CONFIG = TEST_PREFIX + "is.test.cfg";
	private static final String TEST_RESOURCES = TEST_PREFIX + "test.res";
	
	private MoSyncProject project;
	private Set<IResource> testResources = new HashSet<IResource>();

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
		ensureHasTestConfig();
	}

	public IBuildConfiguration ensureHasTestConfig() {
		project.activateBuildConfigurations();
		for (String cfgId : project.getBuildConfigurations()) {
			IBuildConfiguration cfg = project.getBuildConfiguration(cfgId);
			if (cfg != null) {
				boolean isTestConfiguration = PropertyUtil.getBoolean(cfg.getProperties(), IS_TEST_CONFIG);
				if (isTestConfiguration) {
					return cfg;
				}
			}
		}
		
		String testCfgId = BuildConfiguration.createUniqueId(project, "Test");
		IBuildConfiguration testCfg = project.installBuildConfiguration(testCfgId);
		PropertyUtil.setBoolean(testCfg.getProperties(), IS_TEST_CONFIG, true);
		return testCfg;
	}
	
	public void assignTestResource(IResource resource, boolean assign) {
		if (assign) {
			testResources.add(resource);
		} else {
			testResources.remove(resource);
		}
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
