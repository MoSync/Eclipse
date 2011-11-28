package com.mobilesorcery.sdk.capabilities.core.apianalysis;

import java.text.MessageFormat;
import java.util.Set;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.capabilities.core.ICapabilitiesAnalyzer;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.ICapabilities;

public class APICapabilitiesAnalyzer implements ICapabilitiesAnalyzer {

	public APICapabilitiesAnalyzer() {
		
	}
	
	public ICapabilities analyze(IProject project, IProgressMonitor monitor) throws CoreException {
			ICProject cProject = CoreModel.getDefault().create(project);
		monitor.beginTask(MessageFormat.format("Analyzing {0}", project.getName()), 1);
		monitor.worked(1);
		IIndex index = CCorePlugin.getIndexManager().getIndex(cProject);
		try {
			index.acquireReadLock();
		} catch (InterruptedException e) {
			throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, e.getMessage(), e));
		}
		
		try {
			APICapabilitiesAnalyzerASTVisitor astVisitor = new APICapabilitiesAnalyzerASTVisitor(project, APICapabilitiesMap.getInstance());
			astVisitor.setProgressMonitor(monitor);
			astVisitor.setIndex(index);
			cProject.accept(astVisitor);
			return astVisitor.getCapabilities();
		} finally {
			index.releaseReadLock();
			monitor.done();
		}
	}
}
