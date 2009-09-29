package com.mobilesorcery.sdk.internal;

import org.eclipse.core.runtime.CoreException;

import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IPackager;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.profiles.IProfile;

public class NullPackager implements IPackager {

    private static NullPackager instance = new NullPackager();

    public static NullPackager getDefault() {
        return instance;
    }
    
    private NullPackager() {
        
    }
    
    public void createPackage(MoSyncProject project, IProfile targetProfile, IBuildResult buildResult) throws CoreException {
        // Do nothing - at this point, anyhoo.
    }

	public void setParameter(String param, String value) {
        // Do nothing - at this point, anyhoo.
	}    

}
