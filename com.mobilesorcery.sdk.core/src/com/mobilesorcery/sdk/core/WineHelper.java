package com.mobilesorcery.sdk.core;

/**
 * The purpose of this class is to convert paths
 * from unix to wine when applicable.
 * 
 * @author ali
 */
public class WineHelper 
{
	/**
	 * Default drive for accessing Unix files in Wine.
	 */
	private static String DEFAULT_WINE_DRIVE_PREFIX = "Z:";
	private static boolean m_isWindows = false; 

	static 
	{		
		m_isWindows = System.getProperty( "os.name" )
				            .toLowerCase( )
				            .contains( "win" );
	}

	/**
	 * Determines if we are on Windows.
	 * 
	 * @return true if the JVM is run on a Windows platform,
	 * false otherwise.
	 */
	public static boolean isWindows ( )
	{
		return m_isWindows;
	}
	
	/**
	 * Converts the given Unix-style path to a Wine path if required. What
	 * this means is that it is mapped to default drive in Wine in which
	 * the Unix filesystem is mounted, and converts all file separators 
	 * to '\'.
	 * 
	 * @param p Path to convert.
	 * @return The converted path.
	 */
	public static String convPath ( String p )
	{	
		if ( m_isWindows == false ) 
		{
			p = DEFAULT_WINE_DRIVE_PREFIX + p.replace( '/', '\\' );
		}
		
		return p;
	}
}
