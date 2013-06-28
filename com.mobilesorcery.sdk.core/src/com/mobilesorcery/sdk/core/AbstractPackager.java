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

import com.mobilesorcery.sdk.internal.Binutils;
import com.mobilesorcery.sdk.profiles.IProfile;

public abstract class AbstractPackager implements IPackagerDelegate {

	public static boolean shouldUseDebugRuntimes(MoSyncProject project, IBuildVariant variant) {
		IPropertyOwner buildProperties = MoSyncBuilder.getPropertyOwner(
				project, variant.getConfigurationId());
		
		return PropertyUtil
                .getBoolean(buildProperties, MoSyncBuilder.USE_DEBUG_RUNTIME_LIBS);
	}

	@Override
	public String getGenerateMode(IProfile profile) {
		return Binutils.BUILD_MX_MODE;
	}

	@Override
	public String getShortDescription(MoSyncProject project, IProfile profile) {
		return getDefaultShortDescription(profile);
	}

	protected static String getDefaultShortDescription(IProfile profile) {
		return profile.getName();
	}
	
	@Override
	public void buildNative(MoSyncProject project, IBuildSession session,
			IBuildVariant variant, IBuildResult result) throws Exception {
		// Do nothing - at this point, anyhoo.
	}

	protected boolean supportsOutputType(String outputType) {
		return MoSyncBuilder.OUTPUT_TYPE_INTERPRETED.equals(outputType);
	}
	
	public String getOutputType(MoSyncProject project) {
		String rawOutputType = project.getProperty(MoSyncBuilder.OUTPUT_TYPE);
		boolean supportsStaticRecompile = supportsOutputType(MoSyncBuilder.OUTPUT_TYPE_STATIC_RECOMPILATION);
		boolean supportsNative = supportsOutputType(MoSyncBuilder.OUTPUT_TYPE_NATIVE_COMPILE);

		// At this point, the 'interpreted' must be supported by all platforms - may change in the future
		if (MoSyncBuilder.OUTPUT_TYPE_NATIVE_COMPILE.equals(rawOutputType)) {
			if (supportsNative) {
				return MoSyncBuilder.OUTPUT_TYPE_NATIVE_COMPILE;
			} else {
				rawOutputType = MoSyncBuilder.OUTPUT_TYPE_STATIC_RECOMPILATION;
			}
		}
		if (MoSyncBuilder.OUTPUT_TYPE_STATIC_RECOMPILATION.equals(rawOutputType)) {
			if (supportsStaticRecompile) {
				return MoSyncBuilder.OUTPUT_TYPE_STATIC_RECOMPILATION;
			}
		}
		return MoSyncBuilder.OUTPUT_TYPE_INTERPRETED;
	}

}
