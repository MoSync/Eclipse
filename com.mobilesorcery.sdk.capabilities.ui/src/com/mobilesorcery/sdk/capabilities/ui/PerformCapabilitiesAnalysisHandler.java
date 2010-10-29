package com.mobilesorcery.sdk.capabilities.ui;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.ui.handlers.HandlerUtil;

import com.mobilesorcery.sdk.capabilities.core.CapabilitiesPlugin;
import com.mobilesorcery.sdk.capabilities.core.ICapabilitiesAnalyzer;
import com.mobilesorcery.sdk.capabilities.core.ICapabilitiesMatcher;
import com.mobilesorcery.sdk.ui.MoSyncCommandHandler;

public class PerformCapabilitiesAnalysisHandler extends MoSyncCommandHandler {

	public PerformCapabilitiesAnalysisHandler() {
		
	}
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			List<IResource> projects = extractResources(HandlerUtil.getCurrentSelection(event), IResource.PROJECT);
		    ICapabilitiesAnalyzer analyzer = CapabilitiesPlugin.getDefault().createAnalyzer();
		    ICapabilitiesMatcher matcher = CapabilitiesPlugin.getDefault().getMatcher();
			
		    CapabilitiesJob job = new CapabilitiesJob(HandlerUtil.getActiveShell(event), analyzer, matcher, projects.toArray(new IProject[0]));
			job.setUser(true);
			job.schedule();
		} catch (Exception e) {
			throw new ExecutionException(e.getMessage(), e);
		}
		return null;
	}

}
