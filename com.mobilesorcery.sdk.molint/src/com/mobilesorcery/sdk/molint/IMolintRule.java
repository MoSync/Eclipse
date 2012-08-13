package com.mobilesorcery.sdk.molint;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;

public interface IMolintRule {

	public List<IMarker> analyze(IProgressMonitor monitor, MoSyncProject project, IBuildVariant variant) throws CoreException;
	
	public String getId();

	public String getName();
	
	public void setSeverity(int severity);

	public int getSeverity();
	
}
