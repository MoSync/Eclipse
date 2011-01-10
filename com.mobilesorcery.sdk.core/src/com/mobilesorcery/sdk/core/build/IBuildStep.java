package com.mobilesorcery.sdk.core.build;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildState;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.core.IProcessConsole;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.LineReader.ILineHandler;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.internal.PipeTool;
import com.mobilesorcery.sdk.internal.dependencies.IDependencyProvider;

public interface IBuildStep {

	/**
	 * Performs this build step.
	 * @param project
	 * @param session
	 * @param buildState
	 * @param variant
	 * @param diff
	 * @param result
	 * @param resourceFilter
	 * @param monitor
	 * @throws Exception
	 */
    void incrementalBuild(MoSyncProject project, 
    		IBuildSession session, IBuildState buildState, IBuildVariant variant,
    		IFileTreeDiff diff,
    		IBuildResult result,
            IFilter<IResource> resourceFilter, 
            IProgressMonitor monitor) throws Exception;

    /**
     * Returns the id of this build step
     * @return
     */
    String getId();
    
    /**
     * Returns the user-friendly name of this build step
     * @return
     */
    String getName();
    
    /**
     * Returns whether to build this step.
     * @param project
     * @param session
     * @param buildResult
     * @return
     */
	boolean shouldBuild(MoSyncProject project, IBuildSession session, IBuildResult buildResult);
    
	void initConsole(IProcessConsole console);
	
	void initBuildProperties(IPropertyOwner buildProperties);

	void initBuildState(IBuildState buildState);

	void initPipeTool(PipeTool pipeTool);

	void initDefaultLineHandler(ILineHandler linehandler);

	void initDependencyProvider(IDependencyProvider<IResource> dependencyProvider);

}
