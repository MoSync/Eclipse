/**
 * 
 */
package com.mobilesorcery.sdk.internal;

import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;

import com.mobilesorcery.sdk.core.AbstractPackager;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IPackager;
import com.mobilesorcery.sdk.core.IProcessConsole;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.profiles.IProfile;

/**
 * The default 'error' packager (or fallback packager), that
 * will be used if no packager is defined for a specific runtime platform. 
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 *
 */
public class ErrorPackager extends AbstractPackager {

    private static IPackager instance = new ErrorPackager();

    public static IPackager getDefault() {
        return instance;
    }

    public void createPackage(MoSyncProject project, IProfile targetProfile, IBuildResult buildResult) throws CoreException {
        DefaultPackager packager = new DefaultPackager(project, targetProfile, false);
        IProcessConsole console = packager.getConsole();
        String errorMsg = MessageFormat.format("No packager defined for {0} (Runtime platform: {1})", targetProfile,
                targetProfile.getPlatform());
        console.addMessage(errorMsg);
        buildResult.addError(errorMsg);
    }

}