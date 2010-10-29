package com.mobilesorcery.sdk.capabilities.core;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public interface ICapabilitiesAnalyzer {

	public ICapabilities analyze(IProject project, IProgressMonitor monitor) throws CoreException;
		
}
