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
package com.mobilesorcery.sdk.builder.moblin;


import java.io.File;
import java.util.List;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.CoreException;

import com.mobilesorcery.sdk.core.AbstractPackager;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.profiles.IProfile;

import com.mobilesorcery.sdk.builder.linux.PackageBuilder;

/**
 * Plugin entry point. Is responsible for creating debian
 * and RPM packages.
 * 
 * @author Ali Mosavian
 */
public class MoblinPackager
extends AbstractPackager
{
    private DefaultPackager m_internal;


    /**
     * This method is called upon whenever a moblin package
     * is to be built.
     *
     * @param project
     * @param targetProfile Profile information i presume.
     * @param buildResult The build result is returned through this parameter.
     *
     * @throws CoreException Error occured
     */
    public void createPackage ( MoSyncProject project,
                                IProfile targetProfile,
                                IBuildResult buildResult )
    throws CoreException
    {
        String  runtime;
        String  appName;
        PackageBuilder pack;

        // Was used for printing to console
        m_internal = new DefaultPackager( project,
                                          targetProfile,
                                          isFinalizerBuild( ) );


        try
        {
            appName = m_internal.resolve( "%project-name%" );

            // Probably don't want this. Unless the debugger is turned of
            // in release runtimes. What's the point of using debug
            // runtimes just cause the user is running his/hers app in
            // debug mode?
            if ( shouldUseDebugRuntimes( ) == true )
                runtime = "runtime.dbg.tar.gz";
            else
                runtime = "runtime.rel.tar.gz";

            //
            // Set package parameters
            //
            pack = new PackageBuilder( new File( m_internal.resolve( "%runtime-dir%" ), runtime ) );

            pack.setAppName( appName );
            pack.setProgramFile( m_internal.resolve( "%program-output%" ) );
            if ( new File( m_internal.resolve( "%resource-output%" ) ).exists( ) )
                pack.setResorceFile( m_internal.resolve( "%resource-output%" ) );

            // TODO: Proper handling of categories
            pack.setCategories( "Applications" );


            //
            // Set icons here
            // Note: Always set a 48x48 png at the very least!
            //
            /*
            pack.setIconSVG( "path..." )
            pack.setIconPNG( size, "path..." )
             */

            //
            // Create packages
            //
            File outputDir = new File( m_internal.resolve( "%final-output-dir%" ), "package" );
            if ( outputDir.exists( ) == false )
                outputDir.mkdirs( );

            List<String> l = pack.createPackages( outputDir );
            buildResult.setBuildResult( new File( l.get( 0 ) ) );
            /*
            for ( String s : l )
                echoToCon( "Package built: " + s );
             */
        }
        catch ( Exception e )
        {
            StringWriter s = new StringWriter( );
            PrintWriter  pr= new PrintWriter( s );
            e.printStackTrace( pr );

            // Return stack trace in case of error
            throw new CoreException( new Status( IStatus.ERROR,
                                                 "com.mobilesorcery.builder.moblin",
                                                 s.toString( ) ));

        }
    }

    /**
     * A hack that was used during debugging to output information
     *
     * @param s String to output
     */
    private void echoToCon ( String s )
    {
        try
        {
            m_internal.runCommandLine( "cmd", "/c", "echo \"" + s + "\"" );
        }
        catch ( IOException ex )
        {
            Logger.getLogger( MoblinPackager.class.getName() ).log( Level.SEVERE,
                                                                    null, ex );
        }
    }
}
