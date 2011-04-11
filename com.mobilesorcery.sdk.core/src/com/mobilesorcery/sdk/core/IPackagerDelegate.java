package com.mobilesorcery.sdk.core;

import org.eclipse.core.runtime.CoreException;

/**
 * <p>To be used with the extension point com.mobilesorcery.core.packager.</p>
 * <p>A packager is called after compiling and linking.</p>
 * <p>@see {@link DefaultPackager}
 * @see {@link AbstractPackager}
 * @author Mattias Bybro, mattias@bybro.com/mattias.bybro@purplescout.se
 *
 */
public interface IPackagerDelegate {

    final static String EXTENSION_POINT = "com.mobilesorcery.core.packagers";
    
    /**
     * <p>Creates the final package. A precondition is a properly built project.</p>
     * <p>The <code>IBuildResult</code> passed in as a parameter should be given notification
     * of where the packaging (build) result is located. </p> 
     * @param project The project to package for.
     * @param targetProfile The profile that is targeted
     * @param buildResult The <code>IBuildResult</code> used to indicate the file location
     * of the build result (such as the JAR file).
     * @see IBuildResult
     * @throws CoreException If the packaging failed.
     */
    void createPackage(MoSyncProject project, IBuildVariant targetProfile, IBuildResult buildResult) throws CoreException;
    
    /**
     * <p>Sets a parameter to be used by the packager.</p>
     * <p>Typical examples include packager-specific parameters, such as
     * Symbian UIDs, Java ME manifest entries, etc.</p>  
     * @param param The parameter to set
     * @param value The value of the parameter, or <code>null</code>.
     * @throws CoreException
     */
    void setParameter(String param, String value) throws CoreException;

}
