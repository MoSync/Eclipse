package com.mobilesorcery.sdk.core.build;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildState;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.core.IProcessConsole;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncProjectParameterResolver;
import com.mobilesorcery.sdk.core.ParameterResolver;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.LineReader.ILineHandler;
import com.mobilesorcery.sdk.internal.PipeTool;
import com.mobilesorcery.sdk.internal.dependencies.IDependencyProvider;
import com.mobilesorcery.sdk.profiles.IProfile;

public class LinkBuildStep extends AbstractBuildStep {

	public static class Factory extends AbstractBuildStepFactory {

		@Override
		public IBuildStep create() {
			return new LinkBuildStep();
		}

	}

	public static final String ID = "link";

	public LinkBuildStep() {
		setId(ID);
		setName("Link");
	}
	
	@Override
	public void incrementalBuild(MoSyncProject mosyncProject, IBuildSession session,
			IBuildState buildState, IBuildVariant variant, IFileTreeDiff diff,
			IBuildResult result, IFilter<IResource> resourceFilter,
			IProgressMonitor monitor) throws Exception {
		IProcessConsole console = getConsole();
		IPropertyOwner buildProperties = getBuildProperties();
		PipeTool pipeTool = getPipeTool();
		ILineHandler lineHandler = getDefaultLineHandler();
		IProject project = mosyncProject.getWrappedProject();
		IProfile targetProfile = variant.getProfile();
		IPath resource = MoSyncBuilder.getResourceOutputPath(project, variant);
		ParameterResolver resolver = getParameterResolver();

		IPath program = MoSyncBuilder.getProgramOutputPath(project, variant);
        IPath programComb = MoSyncBuilder.getProgramCombOutputPath(project, variant);

        boolean librariesHaveChanged = haveLibrariesChanged(mosyncProject, buildProperties, programComb);
        boolean requiresLinking = librariesHaveChanged || diff == null || !diff.isEmpty();
        if (librariesHaveChanged) {
        	console.addMessage("Libraries have changed, will require re-linking");
        }

        /**
         * Perform linking.
         */
        boolean isLib = MoSyncBuilder.isLib(mosyncProject);
        if (requiresLinking) {
            String[] objectFiles = getObjectFilesForProject(session);
            pipeTool.setInputFiles(objectFiles);
            String pipeToolMode = MoSyncBuilder.getPipeToolMode(targetProfile, isLib);
            pipeTool.setMode(pipeToolMode);
            IPath libraryOutput = MoSyncBuilder.computeLibraryOutput(mosyncProject, buildProperties);
            pipeTool.setOutputFile(isLib ? libraryOutput : program);
            pipeTool.setLibraryPaths(MoSyncBuilder.resolvePaths(MoSyncBuilder.getLibraryPaths(project, buildProperties), resolver));
            pipeTool.setLibraries(MoSyncBuilder.getLibraries(buildProperties));
            boolean elim = !isLib && PropertyUtil.getBoolean(buildProperties, MoSyncBuilder.DEAD_CODE_ELIMINATION);
            pipeTool.setDeadCodeElimination(elim);
            pipeTool.setCollectStabs(true);

            String[] extraLinkerSwitches = PropertyUtil.getStrings(buildProperties, MoSyncBuilder.EXTRA_LINK_SWITCHES);
            pipeTool.setExtraSwitches(extraLinkerSwitches);

            if (objectFiles.length > 0) {
                pipeTool.run();
                
                // If needed, run a second time to generate IL
                if (isLib == false && pipeToolMode.equals(PipeTool.BUILD_C_MODE) == false) {
                	pipeTool.setMode(PipeTool.BUILD_C_MODE);
            		pipeTool.run();
                }
            }

            if (elim) {
                PipeTool elimPipeTool = new PipeTool();
                elimPipeTool.setProject(project);
                elimPipeTool.setLineHandler(lineHandler);
                elimPipeTool.setNoVerify(true);
                elimPipeTool.setGenerateSLD(false);
                elimPipeTool.setMode(PipeTool.BUILD_C_MODE);
                elimPipeTool.setOutputFile(program);
                elimPipeTool.setConsole(console);
                elimPipeTool.setExtraSwitches(extraLinkerSwitches);
                elimPipeTool.setAppCode(MoSyncBuilder.getCurrentAppCode(session));
                elimPipeTool.setArguments(buildProperties);
                File rebuildFile = new File(elimPipeTool.getExecDir(), "rebuild.s");
                elimPipeTool.setInputFiles(new String[] { rebuildFile.getAbsolutePath() });
                elimPipeTool.run();
            }

            if (!isLib) {
                // Create "comb" file - program + resources in one. We'll
                // always
                // make one, even though no resources present.
                ArrayList<File> parts = new ArrayList<File>();
                if (program.toFile().exists()) {
                	parts.add(program.toFile());
                }
                if (getResourceFiles(session).length > 0 && program.toFile().exists() && resource.toFile().exists()) {
                    parts.add(resource.toFile());
                }

                if (parts.size() > 1) {
                    console.addMessage(MessageFormat.format("Combining {0} into one large file, {1}", Util.join(parts.toArray(), ", "), programComb
                            .toFile()));
                }
                Util.mergeFiles(new SubProgressMonitor(monitor, 1), parts.toArray(new File[parts.size()]), programComb.toFile());
            }
        }
	}
	
    private String[] getResourceFiles(IBuildSession session) {
		String[] result = (String[]) session.getProperties().get(ResourceBuildStep.RESOURCE_FILES);
		return result == null ? new String[0] : result;
	}

	private String[] getObjectFilesForProject(IBuildSession session) {
		String[] result = (String[]) session.getProperties().get(CompileBuildStep.OBJECT_FILES);
		return result == null ? new String[0] : result;
	}

	/**
     * Returns true if any of the libraries that the given project depends
     * on have changed.
     * 
     * @param mosyncProject Project to check for changes. 
     * @param buildProperties Build properties of project.
     * @param programComb Latest built program file.
     * @return true if any of the libraries have changed, false otherwise. 
     */
    private boolean haveLibrariesChanged(MoSyncProject mosyncProject, IPropertyOwner buildProperties, IPath programComb)
    {
        long librariesTouched = mosyncProject.getLibraryLookup(buildProperties).getLastTouched();
        long programCombTouched = programComb.toFile().exists() ? programComb.toFile().lastModified() : Long.MAX_VALUE;
        return librariesTouched > programCombTouched;
    }
	
	@Override
	public boolean shouldAdd(IBuildSession session) {
		return session.doLink();
	}
	
	@Override
	public String[] getDependees() {
		return new String[] { CompileBuildStep.ID };
	}

}
