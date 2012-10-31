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
package com.mobilesorcery.sdk.core;

import com.mobilesorcery.sdk.internal.PipeTool;
import com.mobilesorcery.sdk.profiles.IProfile;

public abstract class AbstractPackager implements IPackagerDelegate {

	public boolean shouldUseDebugRuntimes(MoSyncProject project, IBuildVariant variant) {
		IPropertyOwner buildProperties = MoSyncBuilder.getPropertyOwner(
				project, variant.getConfigurationId());
		
		return PropertyUtil
                .getBoolean(buildProperties, MoSyncBuilder.USE_DEBUG_RUNTIME_LIBS);
	}

	@Override
	public String getGenerateMode(IProfile profile) {
		return PipeTool.BUILD_C_MODE;
	}

	@Override
	public String getShortDescription(MoSyncProject project, IProfile profile) {
		return getDefaultShortDescription(profile);
	}

	protected static String getDefaultShortDescription(IProfile profile) {
		return profile.getName();
	}

}
