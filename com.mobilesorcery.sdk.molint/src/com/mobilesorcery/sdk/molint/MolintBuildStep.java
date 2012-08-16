package com.mobilesorcery.sdk.molint;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;

import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.build.AbstractBuildStep;
import com.mobilesorcery.sdk.core.build.AbstractBuildStepFactory;
import com.mobilesorcery.sdk.core.build.CompileBuildStep;
import com.mobilesorcery.sdk.core.build.IBuildStep;

public class MolintBuildStep extends AbstractBuildStep {

	public static class Factory extends AbstractBuildStepFactory {

		@Override
		public IBuildStep create() {
			return new MolintBuildStep(this);
		}

		@Override
		public String getId() {
			return MolintBuildStepExtension.ID;
		}

		@Override
		public String getName() {
			return "MoSync Lint";
		}

	}

	private Factory prototype;

	public MolintBuildStep(Factory prototype) {
		this.prototype = prototype;
		setId(prototype.getId());
		setName(prototype.getName());
	}

	private List<IMolintRule> getRules() {
		return MolintPlugin.getDefault().getAllRules();
	}

	@Override
	public int incrementalBuild(MoSyncProject project, IBuildSession session,
			IBuildVariant variant, IFileTreeDiff diff, IBuildResult result,
			IProgressMonitor monitor) throws Exception {
		if (MolintPlugin.getDefault().isMolintEnabled()) {
			boolean failed = false;
			for (IMolintRule rule : getRules()) {
				if (rule.getSeverity() == IMarker.SEVERITY_WARNING
						|| rule.getSeverity() == IMarker.SEVERITY_ERROR) {
					List<IMarker> analysisResult = rule.analyze(monitor,
							project, variant);
					if (analysisResult != null) {
						for (IMarker potentialError : analysisResult) {
							if (potentialError.getAttribute(IMarker.SEVERITY,
									IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
								failed = true;
								Object lineAttr = potentialError.getAttribute(IMarker.LINE_NUMBER);
								getConsole().addMessage(IMessageProvider.ERROR, 
										MessageFormat.format("{0}:{1}: {2}", 
												potentialError.getResource().getLocation().toFile(),
												lineAttr == null ? "-1" : lineAttr.toString(),
												potentialError.getAttribute(IMarker.MESSAGE)));
							}
						}
					}
				}
			}
			if (failed) {
				throw new CoreException(
						new Status(
								IStatus.ERROR,
								MolintPlugin.PLUGIN_ID,
								"MoLint failed. To configure MoLint checks, go to Preferences and select MoSync Tool > MoLint"));
			}
		}
		return CONTINUE;
	}

	@Override
	public String[] getDependees() {
		return new String[] { CompileBuildStep.ID };
	}

}
