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
package com.mobilesorcery.sdk.builder.s60;

import org.eclipse.core.runtime.CoreException;

import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;

/**
 * Packager for Symbian 3d and 5th edition applications. 
 * 
 * @author Mattias
 */
public class V3Packager 
extends S60Packager 
{
	/**
	 * @see IPackager
	 */
	public void createPackage ( MoSyncProject project, 
            IBuildVariant variant, 
            IBuildResult buildResult )
	throws CoreException
	{
		V2V3Builder builder = new V2V3Builder( PropertyInitializer.S60V3_UID, ".sisx" );
		builder.createPackage( project, variant, buildResult, shouldUseDebugRuntimes( ) );
	}

}
