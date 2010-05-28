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
package com.mobilesorcery.sdk.builder.linux;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.StringTokenizer;

import java.io.File;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.KeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import org.freecompany.redline.Builder;
import org.freecompany.redline.header.Architecture;
import org.freecompany.redline.header.Os;
import org.freecompany.redline.header.RpmType;

import com.mobilesorcery.sdk.builder.linux.deb.BuilderUtil;
import com.mobilesorcery.sdk.builder.linux.deb.DebBuilder;
import com.mobilesorcery.sdk.builder.linux.deb.fields.ArchitectureHeader;
import com.mobilesorcery.sdk.builder.linux.deb.fields.DependsHeader;
import com.mobilesorcery.sdk.builder.linux.deb.fields.DescriptionHeader;
import com.mobilesorcery.sdk.builder.linux.deb.fields.MaintainerHeader;
import com.mobilesorcery.sdk.builder.linux.deb.fields.PriorityHeader;
import com.mobilesorcery.sdk.builder.linux.deb.fields.SectionHeader;
import com.mobilesorcery.sdk.core.Version;




/**
 * This class handles the details of building RPM and DEB packages
 * from a template package.
 *
 *	TODO:
 *		- Fix package signing 
 *		- Fix application category
 *		- Proper package meta data, needs Eclipse support!
 *		-
 *
 * @author Ali Mosavian
 */
public class PackageBuilder
{
	/**
	 * This Enum defines the X windows menu categories,
	 * obviously, not all of them are suitable in MoSync.
	 * 
	 * 
	 * @author Ali Mosavian
	 */
    public enum DesktopEntryCategory
    {
    	AudioVideo, 	// A multimedia (audio/video) application	 
    	Audio,			// An audio application	Desktop entry must include AudioVideo as well
    	Video,			// A video application	Desktop entry must include AudioVideo as well
    	Development,  	// An application for development	 
    	Education, 		// Educational software	 
    	Game,			// A game	 
    	Graphics,		// Graphical application	 
    	Network, 		// Network application such as a web browser	 
    	Office, 		// An office type application	 
    	Settings,		// Settings applications	Entries may appear in a separate menu or as part of a "Control Center"
    	System,			// System application, "System Tools" such as say a log viewer or network monitor	 
    	Utility			// Small utility application, "Accessories"    	
    };
    
	private PrivateKey      m_privKey;
    private File            m_tempDir;
    private File            m_template;
    private PackageParser   m_templateParser;    
    private Map<String,String> m_resourceMap;
    

    /**
     * Constructor
     *
     * @param t Sets the package template
     */
    public PackageBuilder ( File t )
    throws IOException
    {
        m_template      = t;
        m_templateParser = new PackageParser( );
        m_resourceMap   = new HashMap<String, String>( );        
        m_tempDir = File.createTempFile( "pkgbld", System.currentTimeMillis( ) + "" );
        
        m_resourceMap.put( "version", "1.0" );
        m_resourceMap.put( "vendor", "notset" );
    }

    public void setVersion ( String v )
    {
    	m_resourceMap.put( "version", v );
    }
    
    public void setVendor ( String v )
    {
    	m_resourceMap.put( "vendor", v );
    }    

    /**
     * Set the application name.
     *
     * @param s Name
     */
    public void setAppName ( String s )
    {
        m_resourceMap.put( "appname", s );
        m_templateParser.setAppName( s );
    }

    /**
     * Sets the categories of the application
     *
     * @param c The category
     */
    public void setCategory ( DesktopEntryCategory c )
    {
        m_templateParser.setAppCategories( c.toString( ) );
    }

    /**
     * Sets the program file that is to be included.
     * Note: This is not optional. there always has to be
     *       a program file!
     *
     * @param p Path to file
     */
    public void setProgramFile ( String p )
    {
        m_resourceMap.put( "program", p );
    }

    /**
     * Sets the resource file that is to be included.
     *
     * @param p Path to file
     */
    public void setResorceFile ( String p )
    {
        m_resourceMap.put( "resource", p );
    }

    /**
     * Set the path to an SVG icon.
     *
     * @param p Path to file
     */
    public void setIconSVG ( String p )
    {
        m_resourceMap.put( "svg", p );
    }

    /**
     * Set the path to png icons of different sizes.
     * Note: At the very least, a 48x48 png icon always has
     *       to be included.
     *
     * @param s Size, (as in size x size), valid size are
     *        [16, 32, 48, 64, 128, 256]
     *
     * @param p Path to file
     */
    public void setIconPNG ( int s,
                             String p )
    {
        m_resourceMap.put( "png"+s, p );
    }

    /**
     * Sets the working directory where temporary
     * unpacking and customization of a package is
     * done. 
     * 
     * @param p
     */
    public void setWorkDir ( String p )
    {
    	m_tempDir.delete( );
    	m_tempDir = new File( p, "temp" );
    }

    /**
     * This method sets a private RSA key in DER format that is used
     * for signing packages. Currently the packages ARE NOT signed.
     *
     * @param k Path to key file
     *
     * @throws IOException
     * @throws FileNotFoundException
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     */
    public void setDERPrivateKeyRSA ( String k )
    throws IOException,
           FileNotFoundException,
           InvalidKeySpecException,
           NoSuchAlgorithmException
           
    {
        // Read private key DER file
        File f = new File( k );
        DataInputStream dis = new DataInputStream( new FileInputStream( f ) );
        byte[] privKeyBytes = new byte[(int)f.length( )];
        dis.read( privKeyBytes );
        dis.close( );

        KeyFactory keyFactory = KeyFactory.getInstance( "RSA" );

        // Decode private key
        KeySpec privSpec = new PKCS8EncodedKeySpec( privKeyBytes );
        m_privKey = keyFactory.generatePrivate( privSpec );
    }

    /**
     * Attempts to create RPM and DEB packages and returns
     * a list with absolute paths to them. If either of them
     * is missing dependency list (This happens for instance
     * when creating the template package on moblin, in which
     * case there won't be a DEB dependency list as dpkg is
     * missing), then that package won't be created. If neither
     * of them has a dependency list, then neither is created
     * and an exception is thrown.
     *
     * @param o The directory to which the packages should be
     *          output.
     *          
     * @param pt Which kind of package to create, valid values
     * 			 are { "deb", "rpm", "all" }.          
     *
     * @return List of (one or two) string(s) with the absolute
     *         path to the package(s)
     *
     * @throws Exception If neither package has a dependency list
     *         or if an exception occurred while building either
     *         package.
     */
    public List<String> createPackages ( File o, 
    									 String pt )
    throws Exception
    {
        List<String> l = new LinkedList<String>( );

        // Parse and unpack template package
        doParseTemplate( );

        // Check which package types we need to create
        if ( (pt.equals( "deb" ) ||
        	  pt.equals( "rpm" ) ||
        	  pt.equals( "all" )) == false )
        	throw new RuntimeException( "Unknown package type" );

        // Create requested package(s)
        if ( pt.equals( "deb" ) || 
        	 pt.equals( "all" )  ) 
        {
	        // Do we have dependencies for DEB ?
	        if ( m_templateParser.getDependsList( ).isEmpty( ) == false )
	            l.add( new File( o, createDEB( o ) ).getAbsolutePath( ) );
        } 
        
        if ( pt.equals( "rpm" ) || 
        	 pt.equals( "all" ) ) 
        {
	        // Do we have dependencies for RPM ?
	        if ( m_templateParser.getRequiresList( ).isEmpty( ) == false )
	            l.add( new File( o, createRPM( o ) ).getAbsolutePath( ) );
        }

        // An empty list means that there hasn't been any attempt 
        // at creating a package because of missing dependency list.
        if ( l.isEmpty( ) )
            throw new Exception( "Package(s) not built - missing dependency list" );

        return l;
    }


    /**
     * This method creates a RPM package from the customized package
     * that doParseTemplate( ) created.
     *
     * @param o Output directory
     * @return Name of the package that was created
     *
     * @throws Exception If any kind of error occurs
     */
    private String createRPM ( File o )
    throws Exception
    {
        Builder rpmBuilder;
        String  appName = m_resourceMap.get( "appname" )
        							   .toLowerCase( )
        							   .replace( " ", "_" );        
        String	version    = m_resourceMap.get( "version" );
        String	vendorName = m_resourceMap.get( "vendor" );
        
        //
        // Set package parameters
        //
        rpmBuilder = new Builder( );

        //rpmBuilder.addSignature( m_privKey );
        rpmBuilder.setPackage( appName, version, "0" );
        rpmBuilder.setVendor( vendorName );
        rpmBuilder.setType( RpmType.BINARY );
        // FIXME: Doesn't have to be i386
        rpmBuilder.setPlatform( Architecture.I386, Os.LINUX );
        rpmBuilder.setSummary( m_resourceMap.get( "appname" ) );
        rpmBuilder.setDescription( m_resourceMap.get( "appname" ) );
        rpmBuilder.setPackager( "MoSync" );
        rpmBuilder.setProvides( m_resourceMap.get( "appname" ) );        
        addFilesToRpm( rpmBuilder, "/", new File( m_tempDir, "." ) );

        // Package dependencies
        for ( String r : m_templateParser.getRequiresList( ) )
        {
            // The format of an rpm dependency is
            //  - name
            //  - name(ver)
            String n = "";
            String v = "";
            StringTokenizer tok = new StringTokenizer( r, "()" );
            
            n = tok.nextToken( );
            if ( tok.hasMoreTokens( ) == true )
                v = tok.nextToken( );
            
            rpmBuilder.addDependencyMore( n, v );
        }

        //
        // Build and return path to package
        //
        return rpmBuilder.build( o );
    }



    /**
     * This method creates a DEB package from the customised package
     * that doParseTemplate( ) created.
     *
     * @param o Output directory
     * @return Name of the package that was created
     *
     * @throws Exception If any kind of error occurs
     */
    private String createDEB ( File o )
    throws Exception
    {
        DebBuilder  debBuilder;
        String      appName = m_resourceMap.get( "appname" );
        String		version    = m_resourceMap.get( "version" );
        String		vendorName = m_resourceMap.get( "vendor" );        
        String      appSummary = appName;
        String      appDescription = appName;
        
        //
        // Set package parameters
        //
        debBuilder = new DebBuilder( appName, version, "0" );

        // FIXME: Doesn't have to be i386
        debBuilder.addHeader( new ArchitectureHeader( ArchitectureHeader.CpuArch.I386 ) );
        debBuilder.addHeader( new MaintainerHeader( vendorName, "" ) );
        debBuilder.addHeader( new SectionHeader( SectionHeader.DebianSections.Utils ) );
        debBuilder.addHeader( new PriorityHeader( PriorityHeader.Priorities.Optional ) );
        debBuilder.addHeader( new DescriptionHeader( appSummary, appDescription ) );        

        // Package dependencies
        for ( String r : m_templateParser.getDependsList( ) )            
            debBuilder.addHeader( new DependsHeader( r ) );

        // Add files and directories
        addFilesToDeb( debBuilder, "/", new File( m_tempDir, "." ) );

        //
        // Build and return path to package
        //
        return debBuilder.build( o );
    }


    /**
     * This method extracts and parses a template package so that it
     * becomes customized to this application.
     *
     * @throws Exception If there was a file error, or missing values
     *         in the meta data of the template.
     */
    private void doParseTemplate ( )
    throws Exception
    {
        File fin;
        File fot;

        // Parse and unpack template
        m_tempDir.delete( );
        m_tempDir.mkdirs( );
        m_templateParser.doProcessTarGZip( m_tempDir, m_template );

        // Copy program file
        if ( m_resourceMap.containsKey( "program" ) == false )
            throw new Exception( "Program file has not been set" );

        fin = new File( m_resourceMap.get( "program" ) );
        fot = new File( m_tempDir, m_templateParser.getProgramFilePath( ) );
        BuilderUtil.getInstance( ).copyFile( fot, fin );

        // Copy resource file
        if ( m_resourceMap.containsKey( "resource" ) == true )
        {
            fin = new File( m_resourceMap.get( "resource" ) );
            fot = new File( m_tempDir, m_templateParser.getResourceFilePath( ) );
            BuilderUtil.getInstance( ).copyFile( fot, fin );
        }

        // Set SVG icon
        if ( m_resourceMap.containsKey( "svg" ) == true )
        {
            fin = new File( m_resourceMap.get( "svg" ) );
            fot = new File( m_tempDir, m_templateParser.getSVGIconDir( ) );
            BuilderUtil.getInstance( ).copyFile( fot, fin );
        }

        // Set PNG icons
        int iconSizeList[] = { 16, 32, 48, 64, 128, 256 };
        for ( int s : iconSizeList )
        {
            if ( m_resourceMap.containsKey( "png"+s ) == true )
            {
                fin = new File( m_resourceMap.get( "png"+s ) );
                fot = new File( m_tempDir, m_templateParser.getPNGIconDir( s ) );
                BuilderUtil.getInstance( ).copyFile( fot, fin );
            }
        }
    }


    /**
     * This method recursively adds file to an RPM while making sure
     * that the file permissions are correct.
     *
     * @param b Instance of the rpm builder class
     * @param r Relative path to base everything on (in the rpm)
     * @param c File to recursively process
     *
     * @throws IOException Error reading the file.
     * @throws NoSuchAlgorithmException This happens when the md5 and sha1
     *         hashing that the rpm builder does internally fails.
     */
    private void addFilesToRpm ( Builder b, 
                                 String r,
                                 File c )
    throws IOException,
           NoSuchAlgorithmException
    {
        // Update relative path
        if ( c.getName( ).equals( "." ) == false )
            r += (r.isEmpty( ) ? "" : (r.endsWith( "/") ? "" : "/")) + c.getName( );

        // Is it a file?
        if ( c.isFile( ) == true )
        {
            b.addFile( r, c, m_templateParser.getFileMode( r ) );
            return;
        }

        // It's a directory
        for ( File f : c.listFiles( ) )
            addFilesToRpm( b, r, f );
    }


    /**
     * This method recursively adds file to a DEB while making sure
     * that the file permissions are correct.
     *
     * @param b Instance of the deb builder class
     * @param r Relative path to base everything on (in the deb)
     * @param c File to recursively process
     *
     * @throws IOException Error reading the file.
     * @throws NoSuchAlgorithmException This happens when the md5 and sha1
     *         hashing that the rpm builder does internally fails.
     */
    public void addFilesToDeb ( DebBuilder b, 
                                String r,
                                File c )
    throws IOException,
           NoSuchAlgorithmException
    {
        // Update relative path
        if ( c.getName( ).equals( "." ) == false )
            r += (r.isEmpty( ) ? "" : (r.endsWith( "/") ? "" : "/")) + c.getName( );

        // Is it a file?
        if ( c.isFile( ) == true )
        {
            b.addFile( r, c, m_templateParser.getFileMode( r ) );
            return;
        }
        else
            b.addFile( r, c );

        // It's a directory
        for ( File f : c.listFiles( ) )
            addFilesToDeb( b, r, f );
    }
}
