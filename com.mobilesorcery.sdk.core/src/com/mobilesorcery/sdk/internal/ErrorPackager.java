/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
/**
 * 
 */
package com.mobilesorcery.sdk.internal;

import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;

import com.mobilesorcery.sdk.core.AbstractPackager;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildVariant;
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
public class ErrorPackager extends AbstractPackager implements IPackager {

    private static IPackager instance = new ErrorPackager();

    public static IPackager getDefault() {
        return instance;
    }

    public void createPackage(MoSyncProject project, IBuildVariant variant, IBuildResult buildResult) throws CoreException {
        DefaultPackager packager = new DefaultPackager(project, variant);
        IProfile targetProfile = variant.getProfile();
        
        IProcessConsole console = packager.getConsole();
        String errorMsg = MessageFormat.format("No packager defined for {0} (Runtime platform: {1})", targetProfile,
                targetProfile.getPlatform());
        console.addMessage(errorMsg);
        buildResult.addError(errorMsg);
    }

	@Override
	public String getId() {
		return "***error***";
	}

}