package com.mobilesorcery.sdk.molint;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICContainer;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICElementVisitor;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;

public abstract class CPPASTMolintRule extends AbstractMolintRule {

	private class AnalyzeVisitor implements ICElementVisitor {

		private IProgressMonitor monitor;
		private CPPASTMolintRule rule;
		private IIndex index;
		private List<IMarker> result;
		private IBuildVariant variant;
		
		public AnalyzeVisitor(IProgressMonitor monitor, IIndex index,
				IBuildVariant variant, CPPASTMolintRule rule) {
			this.monitor = monitor;
			this.index = index;
			this.variant = variant;
			this.rule = rule;
			result = new ArrayList<IMarker>();
		}

		@Override
		public boolean visit(ICElement element) throws CoreException {
			if (element instanceof ICContainer) {
				return shouldVisit(((ICContainer) element).getResource());
			}
			if (element instanceof ITranslationUnit) {
				ITranslationUnit tu = (ITranslationUnit) element;
				boolean shouldVisit = shouldVisit(tu.getResource());
				if (shouldVisit) {
					IASTTranslationUnit ast = tu.getAST(index,
							ITranslationUnit.AST_SKIP_ALL_HEADERS);
					if (CoreMoSyncPlugin.getDefault().isDebugging()) {
						CoreMoSyncPlugin.trace("Parsing translation unit {0}",
								ast.getFilePath());
					}

					if (ast != null) {
						List<IMarker> partialResult = rule.analyze(MoSyncProject.create(tu.getResource().getProject()), variant, tu, ast);
						if (partialResult != null) {
							result.addAll(partialResult);
						}
					}
				}
				return !isCancelled() && shouldVisit;
			}
			return !isCancelled();
		}

		private boolean isCancelled() {
			return monitor != null && monitor.isCanceled();
		}

		public List<IMarker> getResult() {
			return result;
		}
	}

	protected CPPASTMolintRule(String id, String name) {
		super(id, name);
	}
	
	private boolean shouldVisit(IResource resource) {
		return resource != null
				&& !MoSyncBuilder.isInOutput(resource.getProject(), resource);
	}

	@Override
	public List<IMarker> analyze(IProgressMonitor monitor, MoSyncProject mosyncProject, IBuildVariant variant) throws CoreException {
		IProject project = mosyncProject.getWrappedProject();
		ICProject cProject = CoreModel.getDefault().create(project);
		monitor.beginTask(
				MessageFormat.format("Analyzing {0}", project.getName()), 1);
		monitor.worked(1);
		IIndex index = CCorePlugin.getIndexManager().getIndex(cProject);
		try {
			index.acquireReadLock();
		} catch (InterruptedException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					CoreMoSyncPlugin.PLUGIN_ID, e.getMessage(), e));
		}

		try {
			AnalyzeVisitor visitor = new AnalyzeVisitor(monitor, index, variant, this);
			cProject.accept(visitor);
			// Since we only get C/C++ Problem markers, no need to clear
			// markers (this is done in the compilation step).
			return visitor.getResult();
		} finally {
			index.releaseReadLock();
			monitor.done();
		}
	}

	/**
	 * Returns problem markers from the analysis.
	 * @param tu 
	 * @param ast
	 * @return Problem markers, all being of the 'C/C++ Problem' type
	 * @throws CoreException 
	 */
	protected abstract List<IMarker> analyze(MoSyncProject project, IBuildVariant variant, ITranslationUnit tu, IASTTranslationUnit ast) throws CoreException;

}
