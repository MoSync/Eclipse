package com.mobilesorcery.sdk.molint.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTNodeLocation;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.model.ICModelMarker;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.templates.ProjectTemplate;
import com.mobilesorcery.sdk.core.templates.TemplateManager;
import com.mobilesorcery.sdk.molint.CPPASTMolintRule;
import com.mobilesorcery.sdk.molint.MolintPlugin;

public class MAHeaderRule extends CPPASTMolintRule {

	private static final String RULE_ID = "maheader";

	public MAHeaderRule() {
		super(RULE_ID, "MAHeaders.h Location");
	}

	public List<IMarker> analyze(IProgressMonitor monitor,
			MoSyncProject mosyncProject, IBuildVariant variant)
			throws CoreException {
		// Our analysis only applies to C/C++ project that has resources
		String templateId = mosyncProject
				.getProperty(MoSyncProject.TEMPLATE_ID);
		ProjectTemplate projectTemplate = templateId == null ? null
				: TemplateManager.getDefault().getProjectTemplate(templateId);
		boolean isCppTemplate = projectTemplate != null
				&& ProjectTemplate.DEFAULT_TYPE.equals(projectTemplate
						.getType());

		boolean usesResourcesDir = MoSyncBuilder
				.getResourcesDirectory(mosyncProject.getWrappedProject()) != null;
		final boolean[] hasResourceFiles = new boolean[] { usesResourcesDir };
		mosyncProject.getWrappedProject().accept(new IResourceVisitor() {
			@Override
			public boolean visit(IResource resource) throws CoreException {
				hasResourceFiles[0] |= MoSyncBuilder.isResourceFile(resource)
						&& !MoSyncBuilder.isInOutput(resource.getProject(),
								resource);
				// Once we find one, we are done.
				return !hasResourceFiles[0];
			}
		});

		if (isCppTemplate && hasResourceFiles[0]) {
			return super.analyze(monitor, mosyncProject, variant);
		} else {
			return null;
		}
	}

	@Override
	protected List<IMarker> analyze(MoSyncProject project,
			IBuildVariant variant, ITranslationUnit tu, IASTTranslationUnit ast)
			throws CoreException {
		ArrayList<IMarker> result = new ArrayList<IMarker>();

		IASTPreprocessorIncludeStatement[] directives = ast
				.getIncludeDirectives();
		String maheaderError = null;
		IASTFileLocation errorLocation = null;

		for (int i = 0; i < directives.length && maheaderError == null; i++) {
			IASTPreprocessorIncludeStatement directive = directives[i];
			String includePath = directive.getName().toString();
			boolean foundMAHeaderInclude = includePath.toLowerCase().contains(
					"maheaders");
			// Why not use CDTs resolver? It uses heuristics and may resolve the
			// file to
			// the wrong location.
			IPath includeFile = null;
			if (foundMAHeaderInclude) {
				includeFile = resolveInclude(new Path(includePath), project, variant,
					directive.isSystemInclude(),
					new Path(directive.getContainingFilename()));
			}

			//IFile[] includeFiles = ResourcesPlugin.getWorkspace().getRoot()
			//		.findFilesForLocationURI(new File(includePath).toURI());
			//for (IFile includeFile : includeFiles) {
			//	if (foundMAHeaderInclude
			//			&& includeFile.getProject() == project
			//					.getWrappedProject()) {
					IPath expectedLocation = MoSyncBuilder.getOutputPath(
							project.getWrappedProject(), variant);
					if (includeFile != null && !includeFile.removeLastSegments(1)
							.equals(expectedLocation)) {
						maheaderError = "MAHeaders.h should be auto-generated, found MAHeaders.h inclusion in source path.";
						IASTNodeLocation[] location = directive
								.getNodeLocations();
						if (location.length > 0
								&& location[0] instanceof IASTFileLocation) {
							errorLocation = (IASTFileLocation) location[0];
						}
				//	}
				//}
			}

		}
		if (maheaderError != null) {
			IResource file = tu.getResource();
			IMarker marker = file
					.createMarker(ICModelMarker.C_MODEL_PROBLEM_MARKER);
			marker.setAttribute(IMarker.MESSAGE, maheaderError);
			marker.setAttribute(IMarker.SEVERITY, getSeverity(IMarker.SEVERITY_ERROR));
			if (errorLocation != null) {
				marker.setAttribute(IMarker.LINE_NUMBER,
						errorLocation.getStartingLineNumber());
				marker.setAttribute(IMarker.CHAR_START,
						errorLocation.getNodeOffset());
				marker.setAttribute(
						IMarker.CHAR_END,
						errorLocation.getNodeOffset()
								+ errorLocation.getNodeLength());
			}
			result.add(marker);
		}

		return result;
	}

	private IPath resolveInclude(IPath includePath, MoSyncProject project,
			IBuildVariant variant, boolean systemInclude, IPath baseLocation)
			throws CoreException {
		try {
			List<IPath> paths = new ArrayList<IPath>(Arrays.asList(MoSyncBuilder
					.getBaseIncludePaths(project, variant)));
			// Note: includePaths does NOT include the output path, we add it
			// ourselves.
			paths.add(MoSyncBuilder.getOutputPath(project.getWrappedProject(),
					variant));

			if (!systemInclude) {
				// First add the . directory
				paths.add(0, baseLocation.removeLastSegments(1));

				// Then check the absolute
				if (includePath.toFile().exists()) {
					return includePath;
				}
			}
			for (IPath path : paths) {
				IPath potentialPath = path.append(includePath);
				if (potentialPath.toFile().exists()) {
					return potentialPath;
				}
			}

			return null;

		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR,
					MolintPlugin.PLUGIN_ID, "Could not resolve includes", e));
		}
	}

}
