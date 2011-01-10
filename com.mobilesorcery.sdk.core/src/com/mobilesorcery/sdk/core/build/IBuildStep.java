package com.mobilesorcery.sdk.core.build;

import java.util.Set;

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
     * Returns whether to build this step. This method will be
     * called after all build steps prior to this one has been 
     * executed and before {@link IBuildStep#incrementalBuild(MoSyncProject, IBuildSession, IBuildState, IBuildVariant, IFileTreeDiff, IBuildResult, IFilter, IProgressMonitor)}
     * is called.
     * @param project
     * @param session
     * @param buildResult
     * @return
     */
	boolean shouldBuild(MoSyncProject project, IBuildSession session, IBuildResult buildResult);
    
	/**
	 * Returns whether to add this build step at all for a certain session.
	 * @param session
	 * @return
	 */
	boolean shouldAdd(IBuildSession session);
	
	/**
	 * Returns the set of build steps that this build step depends
	 * on.
	 * @return An array of <code>String</code>s containing
	 * the ids of the build steps that must be executed BEFORE this.
	 * This method may return <code>null</code>.
	 */
	String[] getDependees();
	
	void initConsole(IProcessConsole console);
	
	void initBuildProperties(IPropertyOwner buildProperties);

	void initBuildState(IBuildState buildState);

	void initPipeTool(PipeTool pipeTool);

	void initDefaultLineHandler(ILineHandler linehandler);

	void initDependencyProvider(IDependencyProvider<IResource> dependencyProvider);

}
