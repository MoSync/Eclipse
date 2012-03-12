package com.mobilesorcery.sdk.core.templates;

import org.eclipse.core.runtime.CoreException;

import com.mobilesorcery.sdk.core.MoSyncProject;

public interface IProjectTemplateExtensionDelegate {

	void configureProject(MoSyncProject project) throws CoreException;

}
