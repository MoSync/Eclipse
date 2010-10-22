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
package com.mobilesorcery.sdk.builder.iphoneos;


import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.CoreException;

import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IconManager;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.AbstractPackager;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.Version;



/**
 * Plugin entry point. Is responsible for creating xcode 
 * project for iphone os devices.
 * 
 * @author Ali Mosavian
 */
public class IPhoneOSPackager
extends AbstractPackager
{
	private String m_iphoneBuildLoc;
	
	public IPhoneOSPackager ( ) 
	{
		MoSyncTool tool = MoSyncTool.getDefault( );
		m_iphoneBuildLoc  = tool.getBinary( "iphone-builder" ).toOSString( );		
	}
    


    /**
     * This method is called upon whenever a iphone os package
     * is to be built.
     *
     * @param project
     * @param targetProfile Profile information i presume.
     * @param buildResult The build result is returned through this parameter.
     *
     * @throws CoreException Error occurred
     */
    public void createPackage ( MoSyncProject project,
                                IBuildVariant variant,
                                IBuildResult buildResult )
    throws CoreException
    {
    	String appName;
    	String version;
    	String company;
    	IconManager     icon;
        DefaultPackager intern;
        

        // Was used for printing to console
        intern = new DefaultPackager( project,
                                      variant );

        //
        // Custom parameters
        //
        appName = intern.getParameters( ).get( DefaultPackager.APP_NAME );
        version = intern.getParameters( ).get( DefaultPackager.APP_VERSION );
        company = intern.getParameters( ).get( DefaultPackager.APP_VENDOR_NAME );
        
        
        try
        {
        	String ver = new Version( version ).asCanonicalString( Version.MICRO );
        	File   in  = intern.resolveFile( "%runtime-dir%/template" );
        	File   out = intern.resolveFile( "%package-output-dir%/xcode-proj" );
        	out.mkdirs( );
        	
        	// Create XCode template        	
			int res = intern.runCommandLineWithRes( m_iphoneBuildLoc,
                                                    "generate",
                                                    "-project-name",
                                                    appName,
                                                    "-version",
                                                    ver,
                                                    "-company-name",
                                                    company,
					                                "-input", 
					                                in.getAbsolutePath( ),
					                                "-output",
					                                out.getAbsolutePath( ) );
			
			// Did it run successfully?
			if ( res != 0 )
				return;
			
			// Copy program files to xcode template
			Util.copyFile( new NullProgressMonitor( ), 
				           intern.resolveFile( "%compile-output-dir%/data_section.bin" ), 
				           new File( out, "data_section.bin" ) );
			
			Util.copyFile( new NullProgressMonitor( ), 
			               intern.resolveFile( "%compile-output-dir%/rebuild.build.cpp" ), 
			               new File( out, "Classes/rebuild.build.cpp" ) );
			
			File resources = intern.resolveFile( "%compile-output-dir%/resources" );
			if ( resources.exists( ) == true )
			{
				Util.copyFile( new NullProgressMonitor( ), 
			                   resources, 
			                   new File( out, "resources" ) );
			}
			else
			{
				Util.writeToFile( new File( out, "resources" ), "" );
			}
			
            //
            // Set icons here
            // Note: Always set a 48x48 png at the very least!
            //
            try
            {
                File f;            	
	            icon = new IconManager( intern,
	            						project.getWrappedProject( )
	            						.getLocation( ).toFile( ) );            
	            
	            
	            // Set PNG icons
	            if ( icon.hasIcon( "png" ) == true )
	            {
		            int[] sizes = {57, 72};
		            for ( int s : sizes )
		            {
			            try 
			            {	            
			            	if ( s == 57 )
			            		f = new File( out, "Icon.png" );
			            	else
			            		f = new File( out, "Icon-"+s+".png" );
			            	
			                if ( f.exists( ) == true )
			                	f.delete( );
			                
			                icon.inject( f, s, s, "png" );
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
			
			buildResult.setBuildResult( out );
        }
        catch ( Exception e )
        {
            // Return stack trace in case of error
            throw new CoreException( new Status( IStatus.ERROR,
                                                 "com.mobilesorcery.builder.iphoneos",
                                                 "Failed to build the xcode template. NOTE: Building for iOS devices is only possible from Mac OS X at the moment." ));
        }
    }
}
