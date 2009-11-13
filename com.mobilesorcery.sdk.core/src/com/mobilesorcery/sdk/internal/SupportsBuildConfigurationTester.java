package com.mobilesorcery.sdk.internal;

import org.eclipse.core.expressions.PropertyTester;

import com.mobilesorcery.sdk.core.MoSyncProject;

public class SupportsBuildConfigurationTester extends MoSyncNatureTester {

	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		MoSyncProject project = extractProject(receiver, property, args, expectedValue);
		return project != null && project.isBuildConfigurationsSupported();
	}

}
