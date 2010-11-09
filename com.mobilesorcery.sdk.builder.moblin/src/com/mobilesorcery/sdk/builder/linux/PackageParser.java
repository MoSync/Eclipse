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


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.mobilesorcery.sdk.builder.linux.deb.BuilderUtil;

/**
 * This package unpacks and parses a template package, recursivly
 * resolving variables in file names and files. It also parses the
 * package meta data which is in JSON,
 *
 * @author Ali Mosavian
 */
public class PackageParser
{    
    private Set<String>             m_parseSet;
    private Map<String, String>     m_iconMap;
    private String                  m_binaryPath;
    private VariableResolver        m_varResolver;
    private List<String>            m_dependsList;
    private List<String>            m_requiresList;
    private Map<String, String>     m_scriptMap;
    private Map<String, Integer>    m_filemodeMap;
    private String                  m_programFile;
    private String                  m_resourceFile;

    /**
     * Constructor
     */
    public PackageParser ( )
    {
        m_parseSet    = new HashSet<String>( );
        m_iconMap     = new HashMap<String, String>( );
        m_varResolver = new VariableResolver( );
        m_dependsList = new LinkedList<String>( );
        m_requiresList= new LinkedList<String>( );
        m_scriptMap   = new HashMap<String, String>( );
        m_filemodeMap = new HashMap<String, Integer>( );


        // Sets defaults
        setAppCategories( "" );
    }

    /**
     * Sets the application name variable
     *
     * @param v Name
     */
    public void setAppName ( String v )
    {
        addVariable( "appname", v );
        setSummary( v );
    }

    /**
     * Set the applications summary.
     * Note: Keep it short and no newlines
     *
     * @param v Summary
     */
    public void setSummary ( String v )
    {
        addVariable( "description", v );
    }

    /**
     * Sets application category, see gnome/kde/clutter manual
     *
     * @param v Category
     */
    public void setAppCategories ( String v )
    {
        addVariable( "categories", v );
    }

    /**
     * Returns the directory where the program and resource
     * file needs to be inserted into
     *
     * @return Program dir or null
     */
    public String getProgramDir ( )
    {
        return m_binaryPath;
    }


    /**
     * Returns png icon directory
     *
     * @param s Icon size, usually 16 ( 16x16 ), 32, 64, 128, 256
     * @return Null if no such directory
     */
    public String getPNGIconDir ( int s )
    {
        if ( m_iconMap.containsKey( "png"+s ) == false )
            return null;

        return m_iconMap.get( "png"+s );
    }

    /**
     * Returns svg icon directory
     * Note: If SVG is used, png usually isn't needed.
     *
     * @return Null if no such directory
     */
    public String getSVGIconDir ( )
    {
        if ( m_iconMap.containsKey( "svg" ) == false )
            return null;

        return m_iconMap.get( "svg" );
    }


    /**
     * Sets a variable value pair, this is used to resolve
     * unknown variables during template package processing.
     *
     * @param s Variable name
     * @param v Variable value
     */
    public void addVariable ( String s,
                              String v )
    {
        m_varResolver.addVariable( s.toLowerCase( ), v );
    }

    /**
     * Returns DEB package dependency list
     *
     * @return List of Strings
     */
    public List<String> getDependsList ( )
    {
        return m_dependsList;
    }


    /**
     * Returns RPM package dependency list
     *
     * @return List of Strings
     */
    public List<String> getRequiresList ( )
    {
        return m_requiresList;
    }


    /**
     * Returns unix file mode for every path in the template.
     *
     * @param n File/path name
     * @return file mode
     */
    public int getFileMode ( String n )
    {
        if ( n.charAt( 0 ) == '/' )
            n = n.substring( 1 );

        if ( m_filemodeMap.containsKey( n ) == false )
            return 0x1a4;

        return m_filemodeMap.get( n );
    }

    /**
     * Returns the path to the program file
     *
     */
    public String getProgramFilePath ( )
    {
        return m_programFile;
    }

    /**
     * Returns the path to the resource file
     *
     */
    public String getResourceFilePath ( )
    {
        return m_resourceFile;
    }


    /**
     * Returns a script
     *
     * @param s Name of the script, valid values are
     *          - "preinst"
     *          - "postinst"
     *          - "prerm"
     *          - "postrm"
     *
     * @return Script or null if it doesn't exist
     */
    public String getScript ( String s )
    {
        s = s.toLowerCase( );

        if ( m_scriptMap.containsKey( s ) == false )
            return null;

        return m_scriptMap.get( s );
    }

    /**
     * Extracts a package template and parses and replaces variables
     * in filenames and files.
     *
     * @param o Output directory
     * @param i Input file
     *
     * @throws Exception If recursion is too deep, a variable isn't defined or
     *                   malformed meta data
     * @throws IOException Error reading inputstream
     * @throws ParseException Malformed JSON
     * @throws FileNotFoundException Could not open input file
     */
    public void doProcessTarGZip ( File o,
                                   File i )
    throws Exception,
           IOException,
           ParseException,
           FileNotFoundException
    {
        FileInputStream fis = new FileInputStream( i );
        GZIPInputStream gis = new GZIPInputStream( fis );
        TarArchiveInputStream tis = new TarArchiveInputStream( gis );

        // Remove any old data if any
        if ( o.exists( ) == true )
            o.delete( );

        // Find and parse meta data, this should always be the
        // first file, but it can' be assumed
        while ( true )
        {
            ArchiveEntry e = tis.getNextEntry( );
            if ( e == null )
                break;

            if ( e.getName( ).equals( ".meta/.meta" ) == false )
                continue;

            doParseMeta( tis );
            break;
        }

        // Reset input
        tis.close( );
        gis.close( );
        fis.close( );
        fis = new FileInputStream( i );
        gis = new GZIPInputStream( fis );
        tis = new TarArchiveInputStream( gis );

        // Process and extract files
        while ( true )
        {
            File f;
            ArchiveEntry e = tis.getNextEntry( );

            if ( e == null )
                break;

            // Check if it's a script that we need to load and parse
            if ( e.getName( ).contains( ".meta" ) == true )
            {
                if ( m_scriptMap.containsKey( e.getName( ) ) == true )
                {
                    String name   = m_scriptMap.get( e.getName( ) );
                    String script = m_varResolver.doParseStream( tis );
                    m_scriptMap.put( name, script  );
                    m_scriptMap.remove( e.getName( ) );
                }
                continue;
            }

            // Store its permissions
            String n = m_varResolver.doResolveString( e.getName( ) );            
            m_filemodeMap.put( n, ((TarArchiveEntry )e).getMode( ) );

            // Directory ?
            f = new File( o, n );
            if ( e.isDirectory( ) == true )
            {
                if ( f.exists( ) == false )
                    f.mkdirs( );
                continue;
            }

            // It's a file
            if ( m_parseSet.contains( e.getName( ) ) == true )
                m_varResolver.doParseCopyStream( f, tis );
            else
                BuilderUtil.getInstance( ).copyInputStreamToFile( f, tis, e.getSize( ) );
        }
    }

    

    /**
     * Parses the meta data and (might) set m_iconList, m_binaryDir,
     * and m_parseSet
     *
     * @param i Inputstream which is meta data
     *
     * @throws Exception If recursion is too deep, a variable isn't defined or
     *                   malformed meta data
     * @throws IOException Error reading inputstream
     * @throws ParseException Malformed JSON
     */
    private void doParseMeta ( InputStream i )
    throws Exception,
           IOException,
           ParseException
    {
        Reader r = new BufferedReader( new InputStreamReader( i ) );
        JSONParser jParse = new JSONParser( );
        JSONObject oMain = (JSONObject)jParse.parse( r );

        // Check for program directory
        if ( oMain.containsKey( "programDir" ) == true )
            m_binaryPath = m_varResolver.doResolveString( (String)oMain.get( "programDir" ) );

        // Check for parse file list
        if ( oMain.containsKey( "parseList" ) )
        {
            for ( Object s : ((JSONArray)oMain.get( "parseList" )).toArray( ) )
                m_parseSet.add( (String)s );
        }

        // Check for icon list
        if ( oMain.containsKey( "iconList" ) )
        {
            for ( Object o : ((JSONArray)oMain.get( "iconList" )).toArray( ) )
            {
                String      name;
                JSONObject  oIcon = (JSONObject)o;

                // Get fields
                if ( oIcon.containsKey( "type" ) == false )
                    throw new Exception( "Malformed metadata - No 'type' in icon" );
                name = (String)oIcon.get( "type" );

                if ( name.equals( "png" ) == true )
                {
                    if ( oIcon.containsKey( "size" ) == false )
                        throw new Exception( "Malformed metadata - 'type' is png but no size" );
                    name += oIcon.get( "size" );
                }
                else if ( name.equals( "svg" ) == true )
                {
                    // Don't need to do anything
                }
                else
                    throw new Exception( "Malformed metadata - Unrecognised icon type" );

                // Associate type with path
                if ( oIcon.containsKey( "path" ) == false )
                    throw new Exception( "Malformed metadata - No 'path' in icon" );
                m_iconMap.put( name, m_varResolver.doResolveString( (String)oIcon.get( "path" )));
            }
        }

        // Check for requires list
        if ( oMain.containsKey( "requires" ) )
        {
            for ( Object s : ((JSONArray)oMain.get( "requires" )).toArray( ) )
                m_requiresList.add( (String)s );
        }

        // Check for depends list
        if ( oMain.containsKey( "depends" ) )
        {
            for ( Object s : ((JSONArray)oMain.get( "depends" )).toArray( ) )
                m_dependsList.add( (String)s );
        }

        // Check scripts
        String[] scriptList = { "preinst", "postinst", "prerm", "postrm" };
        for ( String s : scriptList )
            if ( oMain.containsKey( s ) )
                m_scriptMap.put( (String)oMain.get( s ), s );

        // Check for program file (path)
        if ( oMain.containsKey( "programFile" ) )
            m_programFile = m_varResolver.doResolveString( (String)oMain.get( "programFile" ) );

        // Check for resource file (path)
        if ( oMain.containsKey( "resourceFile" ) )
            m_resourceFile = m_varResolver.doResolveString( (String)oMain.get( "resourceFile" ) );
    }

}
