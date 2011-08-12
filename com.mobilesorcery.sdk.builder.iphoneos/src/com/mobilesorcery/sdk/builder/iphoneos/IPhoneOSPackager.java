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
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.AbstractPackager;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IconManager;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.PropertyUtil;
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
	public final static String ID = "com.mobilesorcery.sdk.build.ios.packager";
	
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
    	String cert;
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
        // We do not yet support configuration specific certs.
        cert = PropertyUtil.getBoolean(project, PropertyInitializer.IPHONE_PROJECT_SPECIFIC_CERT) ? 
        		project.getProperty(PropertyInitializer.IPHONE_CERT):
        		Activator.getDefault().getPreferenceStore().getString(PropertyInitializer.IPHONE_CERT);
        
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
                                                    "-cert",
                                                    cert,
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
	            						project );            
	            
	            
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

	            // Now, if we have XCode, build it as well!
	            if (XCodeBuild.getDefault().isValid()) {
	            	out = buildViaXCode(project, intern, variant, out).toFile();
	            } else {
	            	intern.getConsole().addMessage("No XCode, will not build generated project");
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
                                                 "Failed to build the xcode template." ));
        }
    }

	private IPath buildViaXCode(MoSyncProject project, DefaultPackager packager, IBuildVariant variant, File xcodeProject) throws CoreException {
		XCodeBuild xcodeBuild = XCodeBuild.getDefault();
		xcodeBuild.setParameters(packager.getParameters());
		// Kind of hard-coded in the XCode project template.
		String cfgId = variant.getConfigurationId();
		IBuildConfiguration cfg = project.getBuildConfiguration(cfgId);
		boolean isDebugBuild = cfg != null && cfg.getTypes().contains(IBuildConfiguration.DEBUG_TYPE);
		String target = isDebugBuild ? "Debug" : "Release";
		String sdkId = getSDK(variant);
		xcodeBuild.build(new Path(xcodeProject.getAbsolutePath()), target, sdkId);
		
		// Hm, is this always true...?
		String simSuffix = isSimulatorBuild(variant) ? "-iphonesimulator" : "";
		return new Path(packager.resolve(xcodeProject.getAbsolutePath() + "/build/" + target + simSuffix + "/%project-name%.app"));
	}

	private boolean isSimulatorBuild(IBuildVariant variant) {
		return variant.getSpecifiers().containsKey(Activator.IOS_SIMULATOR_SPECIFIER);
	}

	private String getSDK(IBuildVariant variant) throws CoreException {
		String sdkId = null;
		if (isSimulatorBuild(variant)) {
			// Special case: build for the simulator
			SDK sdk = Activator.getDefault().getDefaultSimulatorSDK();
			if (sdk == null) {
				throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "No simulator SDK found, cannot build"));
			}
			sdkId = sdk.getId();
		}
		return sdkId;
	}
}
