package com.mobilesorcery.sdk.core.build;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildState;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.LineReader.ILineHandler;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.ParameterResolverException;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.internal.builder.MoSyncBuilderVisitor;
import com.mobilesorcery.sdk.internal.dependencies.DependencyManager;

public class CompileBuildStep extends AbstractBuildStep {

	public static class Factory extends AbstractBuildStepFactory {

		@Override
		public IBuildStep create() {
			return new CompileBuildStep();
		}

		@Override
		public String getId() {
			return ID;
		}

		@Override
		public String getName() {
			return "Compile";
		}
	}

	public static final String OBJECT_FILES = CompileBuildStep.class.getName() + "obj.files";

	public static final String ID = "compile";

	public CompileBuildStep() {
		setId(ID);
		setName("Compile");
	}

	@Override
	public int incrementalBuild(MoSyncProject mosyncProject, IBuildSession session,
			IBuildVariant variant, IFileTreeDiff diff,
			IBuildResult buildResult, IProgressMonitor monitor) throws CoreException, ParameterResolverException {
		if (isOutputType(mosyncProject, variant, MoSyncBuilder.OUTPUT_TYPE_NATIVE_COMPILE)) {
			getConsole().addMessage("Native compilation");
			return CONTINUE;
		}
		
		IProject project = mosyncProject.getWrappedProject();

        MoSyncBuilderVisitor compilerVisitor = new MoSyncBuilderVisitor();
        compilerVisitor.setProject(project);
        compilerVisitor.setVariant(variant);
        compilerVisitor.setDependencyProvider(getDependencyProvider());

        IPropertyOwner buildProperties = getBuildProperties();
        ILineHandler lineHandler = getDefaultLineHandler();

        compilerVisitor.setConsole(getConsole());
        compilerVisitor.setExtraCompilerSwitches(MoSyncBuilder.getExtraCompilerSwitches(mosyncProject, variant));
        Integer gccWarnings = PropertyUtil.getInteger(buildProperties, MoSyncBuilder.GCC_WARNINGS);
        compilerVisitor.setGCCWarnings(gccWarnings == null ? 0 : gccWarnings.intValue());
        compilerVisitor.setOutputPath(MoSyncBuilder.getOutputPath(project, variant));
        compilerVisitor.setLineHandler(lineHandler);
        compilerVisitor.setBuildResult(buildResult);
        compilerVisitor.setDiff(diff);
        compilerVisitor.setResourceFilter(getResourceFilter());
        compilerVisitor.setParameterResolver(getParameterResolver());
        try {
			compilerVisitor.incrementalCompile(monitor, getBuildState().getDependencyManager(), buildResult.getDependencyDelta());
		} catch (ParameterResolverException e) {
			throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, e.getMessage(), e));
		}

        /* Update dependencies */
        IResource[] allAffectedResources = compilerVisitor.getAllAffectedResources();
        Set<IProject> projectDependencies = computeProjectDependencies(monitor, mosyncProject, getBuildState(), allAffectedResources);
        DependencyManager<IProject> projectDependencyMgr = CoreMoSyncPlugin.getDefault().getProjectDependencyManager(ResourcesPlugin.getWorkspace());
        projectDependencyMgr.setDependencies(project, projectDependencies);

        // TODO: Better way to transport this stuff?
        session.getProperties().put(OBJECT_FILES, compilerVisitor.getObjectFilesForProject(project));

        return CONTINUE;
	}

}
