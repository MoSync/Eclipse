package com.mobilesorcery.sdk.html5.ui;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.ui.handlers.HandlerUtil;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.build.BuildSequence;
import com.mobilesorcery.sdk.core.build.BundleBuildStep;
import com.mobilesorcery.sdk.core.build.IBuildStepFactory;
import com.mobilesorcery.sdk.html5.HTML5DebugSupportBuildStep;
import com.mobilesorcery.sdk.html5.HTML5DebugSupportBuildStepExtension;
import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.ui.MoSyncCommandHandler;

public class DebuggingEnableHandler extends MoSyncCommandHandler {

	public final static String ENABLE_ID = "com.mobilesorcery.sdk.html5.enable.debugger";
	public final static String DISABLE_ID = "com.mobilesorcery.sdk.html5.disable.debugger";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		boolean enable = ENABLE_ID.equals(event.getCommand().getId());

		IProject project = (IProject) extractFirstResource(
				HandlerUtil.getCurrentSelection(event),
				IResource.PROJECT);
		MoSyncProject mosyncProject = MoSyncProject.create(project);
		
		try {
			if (enable) {
				BuildSequence seq = BuildSequence.getCached(mosyncProject);
				List<IBuildStepFactory> steps = seq.getBuildStepFactories();
				List<IBuildStepFactory> newSteps = new ArrayList<IBuildStepFactory>();
				newSteps.add(new HTML5DebugSupportBuildStep.Factory());
				for (IBuildStepFactory step : steps) {
					if (!isLegacyHTML5BundleStep(step)
							&& !step.getId().equals(
									HTML5DebugSupportBuildStepExtension.ID)) {
						newSteps.add(step);
					}
				}
				PropertyUtil.setBoolean(mosyncProject,
						Html5Plugin.JS_PROJECT_SUPPORT_PROP, true);

				seq.apply(newSteps);
			}
			Html5Plugin.getDefault().setJSODDEnabled(mosyncProject, enable);
		} catch (IOException e) {
			throw new ExecutionException(MessageFormat.format(
					"Could not enable debugging for project {0}. Reason: {1}",
					project.getName(), e.getMessage()));
		}
		return null;
	}

	private boolean isLegacyHTML5BundleStep(IBuildStepFactory step) {
		if (BundleBuildStep.ID.equals(step.getId())) {
			// Apply some heuristics.
			BundleBuildStep.Factory bundleStep = (BundleBuildStep.Factory) step;
			return "%current-project%/LocalFiles"
					.equals(bundleStep.getInFile());
		}
		return false;
	}

}
