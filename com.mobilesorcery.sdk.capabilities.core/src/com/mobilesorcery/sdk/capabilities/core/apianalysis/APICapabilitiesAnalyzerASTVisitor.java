package com.mobilesorcery.sdk.capabilities.core.apianalysis;

import java.net.URI;
import java.util.Arrays;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexFile;
import org.eclipse.cdt.core.index.IIndexFileLocation;
import org.eclipse.cdt.core.index.IIndexName;
import org.eclipse.cdt.core.model.ICContainer;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICElementVisitor;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import com.mobilesorcery.sdk.core.Capabilities;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.ICapabilities;
import com.mobilesorcery.sdk.core.MoSyncBuilder;

public class APICapabilitiesAnalyzerASTVisitor extends ASTVisitor implements
		ICElementVisitor {

	private IIndex index;
	private final Capabilities requiredCapabilites = new Capabilities();
	private final IProject project;
	private final APICapabilitiesMap capabilitesMap;
	private IProgressMonitor monitor;

	public APICapabilitiesAnalyzerASTVisitor(IProject project, APICapabilitiesMap capabilitiesMap) {
		super(true);
		this.project = project;
		this.capabilitesMap = capabilitiesMap;
	}

	public void setIndex(IIndex index) {
		this.index = index;
	}

	@Override
	public int visit(IASTName name) {
		IBinding binding = name.resolveBinding();

		// Our heurisitic is: if a file DIRECTLY refers to a
		// capability-requiring API,
		// then we'll add it. (For example calling an API that in its turn
		// refers
		// to a capability-requiring API is not supported; we expect the setup
		// of the
		// capability requirement API map will handle this.
		if (binding != null
				&& shouldVisit(new Path(name.getContainingFilename()))) {
			try {
				IIndexName[] decl = index.findDefinitions(binding);
				for (int i = 0; i < decl.length; i++) {
					IIndexFile indexFile = decl[i].getFile();
					if (indexFile != null) {
						IIndexFileLocation location = indexFile.getLocation();
						// if (shouldVisit(location)) {
						char[] id = decl[i].getSimpleID();
						ICapabilities capabilites = getCapabilites(
								location.getURI(), new String(id));
						if (capabilites != null) {
							// dump(name, 0);
							requiredCapabilites.copyMerge(capabilites);
						}
						// }
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return isCancelled() ? PROCESS_ABORT : PROCESS_CONTINUE;
	}

	private boolean isCancelled() {
		return monitor != null && monitor.isCanceled();
	}

	private void dump(IASTNode node, int indent) {
		IASTNode parent = node.getParent();
		if (parent != null) {
			dump(parent, indent + 2);
		}
		char[] spaces = new char[indent];
		Arrays.fill(spaces, ' ');
		System.out.println(new String(spaces) + node.getContainingFilename());
	}

	@Override
	public int visit(IASTDeclaration declaration) {
		return PROCESS_CONTINUE; /*(declaration instanceof ICPPASTLinkageSpecification) ? PROCESS_SKIP
				: PROCESS_CONTINUE;*/
	}

	public ICapabilities getCapabilities() {
		return requiredCapabilites;
	}

	private ICapabilities getCapabilites(URI location, String refid) {
		return capabilitesMap.get(location, refid);
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
				IASTTranslationUnit ast = tu.getAST(index, ITranslationUnit.AST_SKIP_ALL_HEADERS);
				if (CoreMoSyncPlugin.getDefault().isDebugging()) {
					CoreMoSyncPlugin.trace("Parsing translation unit {0}", ast.getFilePath());
				}

				if (ast != null) {
					ast.accept(this);
				}
			}
			return !isCancelled() && shouldVisit;
		}
		return !isCancelled();
	}

	private boolean shouldVisit(IIndexFileLocation location) {
		String path = location.getFullPath();
		if (path != null) {
			return shouldVisit(ResourcesPlugin.getWorkspace().getRoot()
					.findMember(path));
		} else {
			return false;
		}
	}

	private boolean shouldVisit(IPath path) {
		return shouldVisit(ResourcesPlugin.getWorkspace().getRoot()
				.findFilesForLocation(path));
	}

	private boolean shouldVisit(IResource[] resources) {
		for (int i = 0; i < resources.length; i++) {
			if (shouldVisit(resources[i])) {
				return true;
			}
		}

		return false;
	}

	private boolean shouldVisit(IResource resource) {
		return resource != null && !MoSyncBuilder.isInOutput(project, resource);
	}

	public void setProgressMonitor(IProgressMonitor monitor) {
		this.monitor = monitor;
	}
}
