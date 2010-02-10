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
import java.util.Map;
import java.util.Map.Entry;
import java.io.PrintWriter;
import java.io.StringWriter;

//import java.io.IOException;
//import java.util.logging.Level;
//import java.util.logging.Logger;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.IStatus;

import com.mobilesorcery.sdk.core.AbstractPackager;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.internal.builder.MoSyncIconBuilderVisitor;
import com.mobilesorcery.sdk.profiles.IProfile;

import com.mobilesorcery.sdk.builder.linux.PackageBuilder;
import com.mobilesorcery.sdk.builder.linux.PackageBuilder.DesktopEntryCategory;

/**
 * Plugin entry point. Is responsible for creating debian
 * and RPM packages. The name says Moblin, but it is a 
 * general purpose Linux packager really.
 * 
 * @author Ali Mosavian
 */
public class MoblinPackager
extends AbstractPackager
{
    


    /**
     * This method is called upon whenever a moblin package
     * is to be built.
     *
     * @param project
     * @param targetProfile Profile information i presume.
     * @param buildResult The build result is returned through this parameter.
     *
     * @throws CoreException Error occurred
     */
    public void createPackage ( MoSyncProject project,
                                IProfile targetProfile,
                                IBuildResult buildResult )
    throws CoreException
    {    	
        String  runtime;
        String  appName;
        IconManager icon;
        PackageBuilder pack;
        DefaultPackager intern;

        // Was used for printing to console
        intern = new DefaultPackager( project,
                                      targetProfile,
                                      isFinalizerBuild( ) );


        try
        {
            appName = intern.resolve( "%project-name%" );

            if ( shouldUseDebugRuntimes( ) == true )
                runtime = "runtime.dbg.tar.gz";
            else
                runtime = "runtime.rel.tar.gz";

            //
            // Set package parameters
            //
            pack = new PackageBuilder( new File( intern.resolve( "%runtime-dir%" ), runtime ) );

            pack.setAppName( appName );
            pack.setProgramFile( intern.resolve( "%program-output%" ) );
            if ( new File( intern.resolve( "%resource-output%" ) ).exists( ) )
                pack.setResorceFile( intern.resolve( "%resource-output%" ) );

            // TODO: Proper handling of categories
            pack.setCategory( DesktopEntryCategory.Utility );
            
            //
            // Set icons here
            // Note: Always set a 48x48 png at the very least!
            //
            try
            {
                File f;
                File outDir = new File( intern.resolve( "%output-dir%" ) );            	
	            icon = new IconManager( intern,
	            						project.getWrappedProject( )
	            						.getLocation( ).toFile( ) );            
	            
	            // Set SVG icon
	            if ( icon.hasIcon( "svg" ) == true )
	            {
		            try 
		            {
			            f = new File( outDir, "icon.svg" );
			            if ( f.exists( ) == true )
			            	f.delete( );
			            if ( icon.inject( f, 0, 0, "svg" ) == true )
			            	pack.setIconSVG( f.getAbsolutePath( ) );
		            }
		            catch ( Exception e ) 
		            {
		            	buildResult.addError( e.getMessage( ) );
		            }	            
	            }
	            
	            // Set PNG icons
	            if ( icon.hasIcon( "png" ) == true )
	            {
		            int[] sizes = {16, 32, 48, 64, 128, 256};
		            for ( int s : sizes )
		            {
			            try 
			            {	            	
			                f = new File( outDir, "icon"+s+".png" );
			                if ( f.exists( ) == true )
			                	f.delete( );
			                if ( icon.inject( f, s, s, "png" ) == true )
			                	pack.setIconPNG( s, f.getAbsolutePath( ) );
			            }
			            catch ( Exception e ) 
			            {
			            	buildResult.addError( e.getMessage( ) );
			            }
		            }
	            }
            }
            catch ( Exception e ) 
            {
            	buildResult.addError( e.getMessage( ) );
            }

            //
            // Create packages
            //
            File outputDir = new File( intern.resolve( "%package-output-dir%" ) );
            if ( outputDir.exists( ) == false )
                outputDir.mkdirs( );

            Map<String, Object> props = targetProfile.getProperties();
            String p = (String)props.get( "MA_PROF_CONST_PACKAGER" );
            if ( p == null )
            	p = "all";
            p = p.toLowerCase( );
            List<String> l = pack.createPackages( outputDir, p );
            buildResult.setBuildResult( new File( l.get( 0 ) ) );
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
}
