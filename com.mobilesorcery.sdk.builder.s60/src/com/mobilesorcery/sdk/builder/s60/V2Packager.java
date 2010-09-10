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

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.CommandLineBuilder;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;

public class V2Packager 
extends S60Packager 
{
	/**
	 * Absolute path of the packager tool.
	 */
	String m_packagerLoc;

	public V2Packager( ) 
	{
		MoSyncTool tool = MoSyncTool.getDefault( );
		m_packagerLoc = tool.getBinary( "package" ).toOSString();
	}
	
	public void createPackage ( MoSyncProject project, 
            IBuildVariant variant, 
            IBuildResult buildResult )
	throws CoreException
	{
		DefaultPackager internal = new DefaultPackager( project, variant );
		
		CommandLineBuilder cmdBuilder = new CommandLineBuilder( m_packagerLoc );
		
		/* Add program */
		File compileOutDir = internal.resolveFile( "%compile-output-dir%" );
		cmdBuilder.flag( "-p" ).with( new File(compileOutDir, "program") );
		
		/* Add resources */
		File resources = new File( compileOutDir, "resources" );
		if ( resources.exists( ) == true ) 
		{
			cmdBuilder.flag( "-r" ).with( resources );
		}
		
		/* Output dir, model, app ame and vendor */
		File packageOutputDir = internal.resolveFile( "%package-output-dir%" ); //$NON-NLS-1$
		packageOutputDir.mkdirs();
		String appName = internal.getParameters( ).get( DefaultPackager.APP_NAME );
		String vendorName = internal.getProjectProperties( ).getProperty( DefaultPackager.APP_VENDOR_NAME_BUILD_PROP );
		cmdBuilder.flag( "-d" ).with( packageOutputDir )
				  .flag( "-m" ).with( getModel( project.getTargetProfile( ) ) )
				  .flag( "-n" ).with( appName )
				  .flag( "--vendor" ).with( vendorName );
		
		/* Use debug runtime */
		if( shouldUseDebugRuntimes( ) )
		{
			cmdBuilder.flag( "--debug" );
		}
		
		/* Symbian UID */
		String uid = formatUID( project.getProperty( PropertyInitializer.S60V2_UID ) );
		cmdBuilder.flag( "--uid" ).with( uid );
		
		try {
			internal.runCommandLine( cmdBuilder.asArray( ) );
			buildResult.setBuildResult( new File(packageOutputDir, appName + ".sis") );
		}
		catch (IOException e) {
			throw new CoreException( new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage( ), e) );
		}
	}
}
