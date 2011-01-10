package com.mobilesorcery.sdk.core.build;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildState;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.internal.builder.MoSyncResourceBuilderVisitor;

public class ResourceBuildStep extends AbstractBuildStep {

	public static final String RESOURCE_FILES = ResourceBuildStep.class.getName() + "resource.files";
	
	public ResourceBuildStep() {
		setId("resource");
		setName("Build Resources");
	}
	
	@Override
	public void incrementalBuild(MoSyncProject mosyncProject, IBuildSession session,
			IBuildState buildState, IBuildVariant variant, IFileTreeDiff diff,
			IBuildResult result, IFilter<IResource> resourceFilter,
			IProgressMonitor monitor) throws CoreException {
		IProject project = mosyncProject.getWrappedProject();
		MoSyncResourceBuilderVisitor resourceVisitor = new MoSyncResourceBuilderVisitor();
        resourceVisitor.setProject(project);
        resourceVisitor.setVariant(variant);
        resourceVisitor.setPipeTool(getPipeTool());
        IPath resource = MoSyncBuilder.getResourceOutputPath(project, variant);
        resourceVisitor.setOutputFile(resource);
        resourceVisitor.setDependencyProvider(getDependencyProvider());
        resourceVisitor.setDiff(diff);
        resourceVisitor.setResourceFilter(resourceFilter);

        monitor.setTaskName("Assembling resources");
        resourceVisitor.incrementalCompile(monitor, buildState.getDependencyManager());
        
        session.getProperties().put(RESOURCE_FILES, resourceVisitor.getResourceFiles());
	}

	@Override
	public boolean shouldBuild(MoSyncProject project, IBuildSession session, IBuildResult buildResult) {
		return super.shouldBuild(project, session, buildResult) && session.doBuildResources();
	}

}
