package com.mobilesorcery.sdk.core;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;

public class MoSyncProjectParameterResolver extends ParameterResolver {

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
		if (PROJECT_ROOT.equals(getPrefix(key))) {
			String projectName = getParameter(key);
			IProject referencedProject = project.getWrappedProject().getWorkspace().getRoot().getProject(projectName);
			if (referencedProject == null || !referencedProject.exists()) {
				throw new ParameterResolverException(PROJECT_ROOT, String.format("No project with name %s", projectName));
			}
			return referencedProject.getLocation().toOSString();
		}
		
		return fallbackResolver == null ? null : fallbackResolver.get(key);
	}

	@Override
	public List<String> listPrefixes() {
		ArrayList<String> result = new ArrayList<String>();
		if (fallbackResolver != null) {
			result.addAll(fallbackResolver.listPrefixes());
		}
		
		result.add(PROJECT_ROOT);
		return result;
	}
	
	@Override
	public List<String> listAvailableParameters(String prefix) {
		if (PROJECT_ROOT.equals(prefix)) {
			return MoSyncProject.listAllProjects();
		}
		
		return null;
	}

}
