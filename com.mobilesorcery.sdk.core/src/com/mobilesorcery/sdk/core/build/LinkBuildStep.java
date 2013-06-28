package com.mobilesorcery.sdk.core.build;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.IProcessConsole;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.ParameterResolver;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.LineReader.ILineHandler;
import com.mobilesorcery.sdk.internal.Binutils;
import com.mobilesorcery.sdk.profiles.IProfile;

public class LinkBuildStep extends AbstractBuildStep {

	public static class Factory extends AbstractBuildStepFactory {

		@Override
		public IBuildStep create() {
			return new LinkBuildStep();
		}

		@Override
		public String getId() {
			return ID;
		}

		@Override
		public String getName() {
			return "Link";
		}

	}

	public static final String ID = "link";

	public LinkBuildStep() {
		setId(ID);
		setName("Link");
	}

	@Override
	public int incrementalBuild(MoSyncProject mosyncProject, IBuildSession session,
			IBuildVariant variant, IFileTreeDiff diff,
			IBuildResult result, IProgressMonitor monitor) throws Exception {
		if (isOutputType(mosyncProject, variant, MoSyncBuilder.OUTPUT_TYPE_NATIVE_COMPILE)) {
			getConsole().addMessage("Native compilation");
			return CONTINUE;
		}

		int continueFlag = IBuildStep.CONTINUE;

		IProcessConsole console = getConsole();
		IPropertyOwner buildProperties = getBuildProperties();
		Binutils binutils = getBinutils();
		binutils.setParameterResolver(getParameterResolver());
		ILineHandler lineHandler = getDefaultLineHandler();
		IProject project = mosyncProject.getWrappedProject();
		IProfile targetProfile = variant.getProfile();
		IPath resource = MoSyncBuilder.getResourceOutputPath(project, variant);
		ParameterResolver resolver = getParameterResolver();

        boolean isLibOrExt = MoSyncBuilder.isLib(mosyncProject) || MoSyncBuilder.isExtension(mosyncProject);
		IPath program = MoSyncBuilder.getProgramOutputPath(project, variant);
        IPath programComb = MoSyncBuilder.getProgramCombOutputPath(project, variant);
        IPath libraryOutput = isLibOrExt ? MoSyncBuilder.computeLibraryOutput(mosyncProject, buildProperties) : null;

        boolean librariesHaveChanged = haveLibrariesChanged(mosyncProject, variant, buildProperties, programComb);
        boolean requiresLinking = librariesHaveChanged || diff == null || !diff.isEmpty();
        if (librariesHaveChanged) {
        	console.addMessage("Libraries have changed, will require re-linking");
        }

        /**
         * Perform linking.
         */
        if (requiresLinking) {
            String[] objectFiles = getObjectFilesForProject(session);
            binutils.setInputFiles(objectFiles);
            String binutilsMode = MoSyncBuilder.getBinutilsMode(mosyncProject, targetProfile, isLibOrExt);
            binutils.setMode(binutilsMode);
            binutils.setOutputFile(isLibOrExt ? libraryOutput : program);
            binutils.setLibraryPaths(MoSyncBuilder.resolvePaths(
							MoSyncBuilder.getLibraryPaths(project, buildProperties,
								mosyncProject.getActiveBuildConfiguration().isType(IBuildConfiguration.DEBUG_TYPE)),
							resolver));
            binutils.setLibraries(MoSyncBuilder.getLibraries(mosyncProject, variant, buildProperties));
            binutils.setLineHandler(lineHandler);

            String[] extraLinkerSwitches = PropertyUtil.getStrings(buildProperties, MoSyncBuilder.EXTRA_LINK_SWITCHES);
            binutils.setExtraSwitches(extraLinkerSwitches);

            continueFlag = binutils.run();

            if (!isLibOrExt) {
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

        IPath buildResult = isLibOrExt ? libraryOutput : programComb;
    	result.setIntermediateBuildResult(ID, buildResult.toFile());

        return continueFlag;
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
	 * @param variant
     * @param buildProperties Build properties of project.
     * @param programComb Latest built program file.
     * @return true if any of the libraries have changed, false otherwise.
     */
	private boolean haveLibrariesChanged(MoSyncProject mosyncProject, IBuildVariant variant,
		IPropertyOwner buildProperties, IPath programComb)
	{
		long librariesTouched = mosyncProject.getLibraryLookup(variant, buildProperties,
			mosyncProject.getActiveBuildConfiguration().isType(IBuildConfiguration.DEBUG_TYPE)
			).getLastTouched();
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
