package com.mobilesorcery.sdk.ui.internal.handlers;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import com.mobilesorcery.sdk.core.MoSyncBuildJob;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.ui.MoSyncCommandHandler;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;

public class FinalizeProjectHandler extends MoSyncCommandHandler implements PropertyChangeListener {

	public FinalizeProjectHandler() {
		MosyncUIPlugin.getDefault().addListener(this);
	}

	@Override
	public void dispose() {
		MosyncUIPlugin.getDefault().removeListener(this);
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		MoSyncProject project = MosyncUIPlugin.getDefault().getCurrentlySelectedProject(HandlerUtil.getActiveWorkbenchWindow(event));
		MoSyncBuildJob job = new MoSyncBuildJob(project, project.getActiveBuildConfiguration());
		job.schedule();
		return null;
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName() == MosyncUIPlugin.CURRENT_PROJECT_CHANGED) {
			MoSyncProject project = MosyncUIPlugin.getDefault().getCurrentlySelectedProject(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
			setBaseEnabled(project != null);
		}
	}


}
