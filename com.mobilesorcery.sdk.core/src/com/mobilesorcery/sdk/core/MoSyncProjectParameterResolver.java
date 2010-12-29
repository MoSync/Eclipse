package com.mobilesorcery.sdk.core;

import org.eclipse.core.resources.IProject;

public class MoSyncProjectParameterResolver implements ParameterResolver {

	//TODO: Variable support?
	//TODO: Support for general split at colon
	private final static String PROJECT_ROOT = "project:";
	
	private MoSyncProject project;

	private ParameterResolver fallbackResolver;

	public MoSyncProjectParameterResolver(MoSyncProject project, ParameterResolver fallbackResolver) {
		this.project = project;
		this.fallbackResolver = fallbackResolver;
	}

	@Override
	public String get(String key) throws ParameterResolverException {
		if (key.startsWith(PROJECT_ROOT)) {
			String projectName = key.substring(PROJECT_ROOT.length());
			IProject referencedProject = project.getWrappedProject().getWorkspace().getRoot().getProject(projectName);
			if (referencedProject == null || !referencedProject.exists()) {
				throw new ParameterResolverException(PROJECT_ROOT, String.format("No project with name %s", projectName));
			}
			return referencedProject.getLocation().toOSString();
		}
		
		return fallbackResolver == null ? null : fallbackResolver.get(key);
	}

}
