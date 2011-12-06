package com.mobilesorcery.sdk.ui.internal.handlers;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.ArrayList;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncBuildJob;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.ui.MoSyncCommandHandler;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;

public class FinalizeProjectHandler extends MoSyncCommandHandler implements PropertyChangeListener {

	private final static String TARGET_PROFILE_SCOPE = "current";
	private final static String ALL_PROFILES_SCOPE = "all";
	private final static String SELECTED_PROFILES_SCOPE = "selected";

	public FinalizeProjectHandler() {
		MosyncUIPlugin.getDefault().addListener(this);
	}

	@Override
	public void dispose() {
		MosyncUIPlugin.getDefault().removeListener(this);
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String scope = event.getParameter("com.mobilesorcery.sdk.ui.finalize.scope");
		MoSyncProject project = MosyncUIPlugin.getDefault().getCurrentlySelectedProject(HandlerUtil.getActiveWorkbenchWindow(event));
		if (project == null) {
			showNoProjectMessage(HandlerUtil.getActiveShell(event));
			return null;
		}
		IProfile[] profilesToBuild = getProfilesToBuild(project, scope);
		IBuildConfiguration cfg = project.getActiveBuildConfiguration();
    	ArrayList<IBuildVariant> variants = new ArrayList<IBuildVariant>();
    	for (IProfile profile : profilesToBuild) {
        	IBuildVariant variant = MoSyncBuilder.getVariant(project, profile, cfg);
        	variants.add(variant);
    	}
    	IBuildSession session = MoSyncBuilder.createFinalizerBuildSession(variants);
		MoSyncBuildJob job = new MoSyncBuildJob(project, session);
		job.schedule();
		return null;
	}

	private void showNoProjectMessage(Shell activeShell) {
		MessageDialog.openError(activeShell, "No project selected", "No project selected. To build, please select a project to build for.");
	}

	private IProfile[] getProfilesToBuild(MoSyncProject project, String scope) {
		if (ALL_PROFILES_SCOPE.equals(scope)) {
			return project.getFilteredProfiles();
		}
		// Default:
		return new IProfile[] { project.getTargetProfile() };
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName() == MosyncUIPlugin.CURRENT_PROJECT_CHANGED) {
			MoSyncProject project = MosyncUIPlugin.getDefault().getCurrentlySelectedProject(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
			setBaseEnabled(project != null);
		}
	}


}
