package com.mobilesorcery.sdk.core;

import org.eclipse.core.runtime.CoreException;

import com.mobilesorcery.sdk.internal.PipeTool;
import com.mobilesorcery.sdk.profiles.IProfile;

/**
 * <p>To be used with the extension point com.mobilesorcery.core.packager.</p>
 * <p>A packager is called after compiling and linking.</p>
 * <p>@see {@link DefaultPackager}
 * @see {@link AbstractPackager}
 * @author Mattias Bybro, mattias@bybro.com/mattias.bybro@purplescout.se
 *
 */
public interface IPackagerDelegate {

	final static String BUILD_C_MODE = PipeTool.BUILD_C_MODE;
	final static String BUILD_GEN_CPP_MODE = PipeTool.BUILD_GEN_CPP_MODE;
	final static String BUILD_GEN_JAVA_MODE = PipeTool.BUILD_GEN_JAVA_MODE;
	final static String BUILD_GEN_CS_MODE = PipeTool.BUILD_GEN_CS_MODE;

    final static String EXTENSION_POINT = "com.mobilesorcery.core.packagers";

    /**
     * <p>Creates the final package. A precondition is a properly built project.</p>
     * <p>The <code>IBuildResult</code> passed in as a parameter should be given notification
     * of where the packaging (build) result is located. </p>
     * @param project The project to package for.
     * @param session The build session
     * @param targetProfile The profile that is targeted
     * @param diff The diff since last package
     * @param buildResult The <code>IBuildResult</code> used to indicate the file location
     * of the build result (such as the JAR file).
     * @see IBuildResult
     * @throws CoreException If the packaging failed.
     */
    void createPackage(MoSyncProject project, IBuildSession session, IBuildVariant targetProfile, IFileTreeDiff diff, IBuildResult buildResult) throws CoreException;

    /**
     * <p>Sets a parameter to be used by the packager.</p>
     * <p>Typical examples include packager-specific parameters, such as
     * Symbian UIDs, Java ME manifest entries, etc.</p>
     * @param param The parameter to set
     * @param value The value of the parameter, or <code>null</code>.
     * @throws CoreException
     */
    void setParameter(String param, String value) throws CoreException;


	/**
	 * Returns the generate mode to be used during linking, as
	 * per defined in the {@link IPackagerDelegate#BUILD_C_MODE},
	 * {@link IPackagerDelegate#BUILD_GEN_CPP_MODE},
	 * {@link IPackagerDelegate#BUILD_GEN_CS_MODE},
	 * {@link IPackagerDelegate#BUILD_GEN_JAVA_MODE}
	 * <p>
	 * <b>Note:</b> We may want to move this responsibility elsewhere,
	 * ie extend the build step factories...?
	 * </p>
	 * @throws CoreException
	 */
	public String getGenerateMode(IProfile profile) throws CoreException;

}
