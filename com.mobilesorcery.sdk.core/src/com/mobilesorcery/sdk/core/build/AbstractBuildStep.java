package com.mobilesorcery.sdk.core.build;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildState;
import com.mobilesorcery.sdk.core.IProcessConsole;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.ParameterResolver;
import com.mobilesorcery.sdk.core.LineReader.ILineHandler;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.internal.PipeTool;
import com.mobilesorcery.sdk.internal.dependencies.DependencyManager;
import com.mobilesorcery.sdk.internal.dependencies.IDependencyProvider;

public abstract class AbstractBuildStep implements IBuildStep {

	private IProcessConsole console;
	private IPropertyOwner buildProperties;
	private IBuildState buildState;
	private PipeTool pipeTool;
	private ILineHandler defaultLineHandler;
	private IDependencyProvider<IResource> dependencyProvider;
	private String id;
	private String name;
	private ParameterResolver resolver;

	public String getId() {
		return id;
	}
	
	protected void setId(String id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	protected void setName(String name) {
		this.name = name;
	}
	
	@Override
	public void initConsole(IProcessConsole console) {
		this.console = console;
	}
	
	protected IProcessConsole getConsole() {
		return console;
	}

	@Override
	public void initBuildProperties(IPropertyOwner buildProperties) {
		this.buildProperties = buildProperties;
	}
	
	protected IPropertyOwner getBuildProperties() {
		return buildProperties;
	}
	
	@Override
	public void initBuildState(IBuildState buildState) {
		this.buildState = buildState;
	}
	
	protected IBuildState getBuildState() {
		return buildState;
	}
	
	public void initPipeTool(PipeTool pipeTool) {
		this.pipeTool = pipeTool;
	}
	
	protected PipeTool getPipeTool() {
		return pipeTool;
	}

	public void initDefaultLineHandler(ILineHandler defaultLineHandler) {
		this.defaultLineHandler = defaultLineHandler;
	}
	
	protected ILineHandler getDefaultLineHandler() {
		return defaultLineHandler;
	}
	
	public void initDependencyProvider(IDependencyProvider<IResource> dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
	}
	
	protected IDependencyProvider<IResource> getDependencyProvider() {
		return dependencyProvider;
	}

	public void initParameterResolver(ParameterResolver resolver) {
		this.resolver = resolver;
	}
	
	public ParameterResolver getParameterResolver() {
		return resolver;
	}

	/**
	 * The default implementation returns <code>true</code> if
	 * the current build has no errors from previous build steps
	 * and if this step should be added (as per {@link #shouldAdd(IBuildSession)})
	 */
	@Override
	public boolean shouldBuild(MoSyncProject project, IBuildSession session, IBuildResult buildResult) {
		return shouldAdd(session) && buildResult.getErrors().isEmpty();
	}
	
	@Override
	public boolean shouldAdd(IBuildSession session) {
		return true;
	}
	
	@Override
	public String[] getDependees() {
		return null;
	}

    protected Set<IProject> computeProjectDependencies(IProgressMonitor monitor, MoSyncProject mosyncProject, IBuildState buildState, IResource[] allAffectedResources) {
        IProject project = mosyncProject.getWrappedProject();
        monitor.setTaskName(MessageFormat.format("Computing project dependencies for {0}", project.getName()));
        DependencyManager<IProject> projectDependencies = CoreMoSyncPlugin.getDefault().getProjectDependencyManager(ResourcesPlugin.getWorkspace());
        projectDependencies.clearDependencies(project);
        HashSet<IProject> allProjectDependencies = new HashSet<IProject>();
        Set<IResource> dependencies = buildState.getDependencyManager().getDependenciesOf(Arrays.asList(allAffectedResources));
        for (IResource resourceDependency : dependencies) {
            if (resourceDependency.getType() != IResource.ROOT) {
                allProjectDependencies.add(resourceDependency.getProject());
            }
        }

        // No deps on self
        allProjectDependencies.remove(project);

        if (CoreMoSyncPlugin.getDefault().isDebugging()) {
            CoreMoSyncPlugin
                    .trace(MessageFormat.format("Computed project dependencies. Project {0} depends on {1}", project.getName(), allProjectDependencies));
        }
        return allProjectDependencies;
    }

    public String toString() {
    	return getId();
    }

}
