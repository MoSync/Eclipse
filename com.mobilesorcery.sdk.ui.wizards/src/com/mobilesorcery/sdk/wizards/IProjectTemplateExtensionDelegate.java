package com.mobilesorcery.sdk.wizards;

import org.eclipse.core.runtime.CoreException;

import com.mobilesorcery.sdk.core.MoSyncProject;

public interface IProjectTemplateExtensionDelegate {

	void configureProject(MoSyncProject project) throws CoreException;

}
