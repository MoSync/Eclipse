package com.mobilesorcery.sdk.internal;

import com.mobilesorcery.sdk.core.MoSyncProject;

public class SupportsBuildConfigurationTester extends MoSyncNatureTester {

    public final static String TYPE = "config-type";
    public final static String ID = "config-id";
    
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		MoSyncProject project = extractProject(receiver, property, args, expectedValue);
		boolean cfgsSupported = project != null && project.areBuildConfigurationsSupported();
		if (cfgsSupported && ID.equals(property)) {
		    return project.getBuildConfigurations().contains(expectedValue);
		} else if (cfgsSupported && TYPE.equals(property)) {
		    return !project.getBuildConfigurationsOfType((String) expectedValue).isEmpty();
		}
		
		return cfgsSupported;
	}

}
