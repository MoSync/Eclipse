package com.mobilesorcery.sdk.core.build;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IMemento;

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

	public static final String ID = "resource";
	public static final String RESOURCE_FILES = ResourceBuildStep.class.getName() + "resource.files";

	public static class Factory extends AbstractBuildStepFactory {

		@Override
		public IBuildStep create() {
			return new ResourceBuildStep();
		}

		@Override
		public String getId() {
			return ID;
		}
		
		@Override
		public String getName() {
			return "Compile resources";
		}
	}

	public ResourceBuildStep() {
		setId(ID);
		setName("Build Resources");
	}
	
	@Override
	public int incrementalBuild(MoSyncProject mosyncProject, IBuildSession session,
			IBuildState buildState, IBuildVariant variant, IFileTreeDiff diff,
			IBuildResult result, IFilter<IResource> resourceFilter,
			IProgressMonitor monitor) throws CoreException {
		IProject project = mosyncProject.getWrappedProject();
		MoSyncResourceBuilderVisitor resourceVisitor = new MoSyncResourceBuilderVisitor();
        resourceVisitor.setProject(project);
        resourceVisitor.setVariant(variant);
        resourceVisitor.setPipeTool(getPipeTool());
        resourceVisitor.setParameterResolver(getParameterResolver());
        IPath resource = MoSyncBuilder.getResourceOutputPath(project, variant);
        resourceVisitor.setOutputFile(resource);
        resourceVisitor.setDependencyProvider(getDependencyProvider());
        resourceVisitor.setDiff(diff);
        resourceVisitor.setResourceFilter(resourceFilter);

        monitor.setTaskName("Assembling resources");
        resourceVisitor.incrementalCompile(monitor, buildState.getDependencyManager());
        
        session.getProperties().put(RESOURCE_FILES, resourceVisitor.getResourceFiles());
        
        return CONTINUE;
	}

	@Override
	public boolean shouldAdd(IBuildSession session) {
		return session.doBuildResources();
	}

}
