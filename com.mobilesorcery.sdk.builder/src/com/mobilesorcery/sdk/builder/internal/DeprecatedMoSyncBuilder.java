package com.mobilesorcery.sdk.builder.internal;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncNature;
import com.mobilesorcery.sdk.core.MoSyncProject;

public class DeprecatedMoSyncBuilder extends IncrementalProjectBuilder {

	public DeprecatedMoSyncBuilder() {
		// TODO Auto-generated constructor stub
	}

	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
		if (!MoSyncNature.hasNature(getProject()) && MoSyncNature.isCompatible(getProject())) {
			throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, Messages.DeprecatedMoSyncBuilder_0));
		}
		
		return new IProject[0];
	}

}
