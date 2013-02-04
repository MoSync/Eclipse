package com.mobilesorcery.sdk.extensionsupport.internal.handlers;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.handlers.HandlerUtil;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.extensionsupport.ExtensionCompiler;
import com.mobilesorcery.sdk.ui.MoSyncCommandHandler;

public class GenerateStubsHandler extends MoSyncCommandHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<IResource> projects = extractResources(HandlerUtil.getCurrentSelection(event));
		List<MoSyncProject> mosyncProjects = MoSyncProject.create(projects);
		for (MoSyncProject mosyncProject : mosyncProjects) {
			try {
				ExtensionCompiler.getDefault().generateStubs(mosyncProject);
				mosyncProject.getWrappedProject().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
			} catch (CoreException e) {
				throw new ExecutionException(e.getMessage(), e);
			}
		}
		return null;
	}


}
