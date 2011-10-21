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
package com.mobilesorcery.sdk.internal;

import org.eclipse.core.runtime.CoreException;

import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.IPackager;
import com.mobilesorcery.sdk.core.MoSyncProject;

public class NullPackager implements IPackager {

    private static NullPackager instance = new NullPackager();

    public static NullPackager getDefault() {
        return instance;
    }

    private NullPackager() {

    }

	@Override
	public void createPackage(MoSyncProject project, IBuildSession session,
			IBuildVariant targetProfile, IFileTreeDiff diff,
			IBuildResult buildResult) throws CoreException {
		// Do nothing - at this point, anyhoo.
    }

	@Override
	public void setParameter(String param, String value) {
        // Do nothing - at this point, anyhoo.
	}

	@Override
	public String getId() {
		return "***null***";
	}

	@Override
	public String getPlatform() {
		return getId();
	}

}
