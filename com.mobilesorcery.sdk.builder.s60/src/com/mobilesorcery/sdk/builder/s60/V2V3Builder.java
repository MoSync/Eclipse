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
import com.mobilesorcery.sdk.profiles.IProfile;

/**
 * A class that contains the shared code between the 2nd and 3d edition
 * Symbian packagers.
 * 
 * @author Mattias
 */
public class V2V3Builder {
	/**
	 * Absolute path of the packager tool.
	 */
	String m_packagerLoc;
	
	/**
	 * String that contains the Symbian uid string, should be taken from
	 * PropertyInitalizer.
	 */
	String m_uidString;
	
	/**
	 * File extension from the resulting file.
	 */
	String m_resultExtension;
	
	/**
	 * Creates a builder for either Symbian 2nd or 3d edition. 
	 * 
	 * @param uidString Defines which Symbian UID string to use.
	 * @param resultExtension The extension of the resulting package.
	 */
	public V2V3Builder(String uidString, String resultExtension)
	{
		MoSyncTool tool = MoSyncTool.getDefault( );
		m_packagerLoc = tool.getBinary( "package" ).toOSString();
		
		m_uidString = uidString;
		m_resultExtension = resultExtension;
	}
	
	/**
	 * @see IPackager
	 * @param shouldUseDebugRuntimes Determines if the debug runtimes
	 *        should be used or not.
	 */
	public void createPackage ( MoSyncProject project, 
            IBuildVariant variant, 
            IBuildResult buildResult,
            boolean shouldUseDebugRuntimes
            )
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
		
		/* Output dir, model, app name and vendor */
		File packageOutputDir = internal.resolveFile( "%package-output-dir%" ); //$NON-NLS-1$
		packageOutputDir.mkdirs();
		String appName = internal.getParameters( ).get( DefaultPackager.APP_NAME );
		String vendorName = internal.getParameters( ).get( DefaultPackager.APP_VENDOR_NAME );
		cmdBuilder.flag( "-d" ).with( packageOutputDir )
				  .flag( "-m" ).with( getModel( variant.getProfile( ) ) )
				  .flag( "-n" ).with( appName )
				  .flag( "--vendor" ).with( vendorName );
		
		/* Use debug runtime */
		if( shouldUseDebugRuntimes )
		{
			cmdBuilder.flag( "--debug" );
		}
		
		/* Symbian UID */
		String uid = formatUID( project.getProperty( m_uidString ) );
		cmdBuilder.flag( "--uid" ).with( uid );
		
		try {
			internal.runCommandLine( cmdBuilder.asArray( ) );
			buildResult.setBuildResult( new File(packageOutputDir, appName + m_resultExtension) );
		}
		catch (IOException e) {
			throw new CoreException( new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage( ), e) );
		}
	}
	
	/**
	 * Returns the model string for the given target profile.
	 * 
	 * @param targetProfile The target profile to extract the UID from.
	 * @return Model string for the given target profile.
	 */
	public static String getModel(IProfile targetProfile)
	{
		return targetProfile.getVendor( ) + "/" + targetProfile.getName( );
	}
	
	/**
	 * Removes 0x in front of the UID if necessary.
	 * 
	 * @param project To look
	 * @return The UID associated with the project.
	 */
	public static String formatUID(String uid)
	{	
		if ( uid.startsWith( "0x" ) ) { //$NON-NLS-1$
			uid = uid.substring( 2 );
		}
		
		return uid;
	}
}
