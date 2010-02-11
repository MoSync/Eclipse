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
package com.mobilesorcery.sdk.builder.linux.deb;


import java.io.File;
import java.util.Map;
import java.util.List;
import java.util.Vector;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;
import java.security.NoSuchAlgorithmException;

import java.io.IOException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;

import org.apache.commons.compress.archivers.ar.*;
import org.apache.commons.compress.archivers.tar.*;
import org.apache.commons.compress.compressors.gzip.*;

import com.mobilesorcery.sdk.builder.linux.deb.fields.Header;
import com.mobilesorcery.sdk.builder.linux.deb.fields.NameHeader;
import com.mobilesorcery.sdk.builder.linux.deb.fields.SizeHeader;
import com.mobilesorcery.sdk.builder.linux.deb.fields.VersionHeader;

import com.mobilesorcery.sdk.builder.linux.deb.fields.ArchitectureHeader;



/**
 * This class is used for building .deb packages.
 * 
 * @author Ali Mosavian
 */
public class DebBuilder
{
    String                      m_packName;
    StringBuilder               m_md5sums;
    private Map<String, Header> m_headerMap;
    private Map<String, String> m_scriptMap;
    private long                m_installedSize;
    private List<SimpleEntry<File, SimpleEntry<String, Integer>>> m_fileList;

    /**
     * Constructor
     *
     */
    public DebBuilder ( String n,
                        String v,
                        String r )
    {
        r = r.isEmpty( ) ? "0" : r;

        m_installedSize = 0;
        m_packName  = n.toLowerCase( )
                       .replaceAll( " ", "_")
                       .concat( "_" )
                       .concat( v )
                       .concat( "-" )
                       .concat( r );
        m_md5sums   = new StringBuilder( );
        m_headerMap = new HashMap<String, Header>( );
        m_scriptMap = new HashMap<String, String>( );
        m_fileList  = new LinkedList<SimpleEntry<File, SimpleEntry<String, Integer>>>( );
        
        // Set name and version
        try
        {            
            addHeader( new NameHeader( n ) );
            addHeader( new VersionHeader( v+"-"+r ) );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
        
    }

    /**
     * Adds header to the control file
     * 
     * @param h Header to add
     *
     * @throws Exception if multiple values of the same header type is
     *         added and the header type doesn't support multiple values
     */
    public void addHeader ( Header h )
    throws Exception
    {
        if ( m_headerMap.containsKey( h.getName( ) ) == false )
            m_headerMap.put( h.getName( ), h );
        else
            m_headerMap.get( h.getName( ) ).addNext( h );

        if ( h instanceof ArchitectureHeader )
            m_packName += "_" + ((ArchitectureHeader)h).getValue( );
    }


    /**
     * Adds a new file to package
     *
     * @param p Path to file
     */
    public void addFile ( String p )
    throws IOException,
           FileNotFoundException,
           NoSuchAlgorithmException
    {
        File f = new File( p );
        int  m = TarArchiveEntry.DEFAULT_FILE_MODE;
        if ( f.isDirectory( ) == true )
            m = TarArchiveEntry.DEFAULT_DIR_MODE;

        addFile( p, f, m );
    }


    /**
     * Adds a new file to package
     *
     * @param p Path to file
     * @param m Standard unix file mode in octal
     */
    public void addFile ( String p,
                          Integer m )
    throws IOException,
           FileNotFoundException,
           NoSuchAlgorithmException
    {
        addFile( p, new File( p ), m );
    }


    /**
     * Adds a new file to package
     *
     * @param f The actual file to add
     * @param p Path to file
     */
    public void addFile ( String p,
                          File f )
    throws IOException,
           FileNotFoundException,
           NoSuchAlgorithmException
    {
        int  m = TarArchiveEntry.DEFAULT_FILE_MODE;
        if ( f.isDirectory( ) == true )
            m = TarArchiveEntry.DEFAULT_DIR_MODE;

        addFile( p, f, m );
    }



    /**
     * Adds a new file to package
     *
     * @param path File path in debian package
     * @param file The actual file to add
     * @param mode Standard unix file mode in octal
     */
    public void addFile ( String path,
                          File file,
                          Integer mode )
    throws IOException,
           FileNotFoundException,
           NoSuchAlgorithmException
    {
        StringBuilder o = m_md5sums;
        
		// These entries will corrupt the dpkg database
        if ( path.equals( "/" ) ||  path.equals( "." ) || path.equals( "./" ) )
            return;
			
        // Add to file list
        if ( path.startsWith( "./" ) == false )
        {
            if ( path.charAt( 0 ) == '/' )
                path = "." + path;
            else
                path = "./" + path;
        }		
        SimpleEntry<String, Integer> v = new SimpleEntry<String, Integer>( path, mode );
        m_fileList.add( new SimpleEntry<File, SimpleEntry<String, Integer>>( file, v ) );

        if ( file.isDirectory( ) == true )
            return;

        // Calculate file MD5
        String md5 = BuilderUtil.getInstance( ).calcFileMD5Sum( file );
        o.append( md5 );
        for ( int i = 0; i < 1+(32-md5.length( )); i++ )
            o.append( " " );
        o.append( path ).append( "\n" );

        // Add file size to total
        m_installedSize += file.length( )/1024;
    }


    /**
     * Sets the pre install script
     *
     * @param s The script
     */
    public void setScriptPreInst ( String s )
    {
        m_scriptMap.put( "preinst", s );
    }

    /**
     * Sets the post install script
     *
     * @param s The script
     */
    public void setScriptPostInst ( String s )
    {
        m_scriptMap.put( "postinst", s );
    }

    /**
     * Sets the pre remove script
     *
     * @param s The script
     */
    public void setScriptPreRm ( String s )
    {
        m_scriptMap.put( "prerm", s );
    }

    /**
     * Sets the post remove script
     *
     * @param s The script
     */
    public void setScriptPostRm ( String s )
    {
        m_scriptMap.put( "postrm", s );
    }


    /**
     * Create debian package
     *
     * @param p Path to write to
     */
    public String build ( File p )
    throws Exception,
           IOException,
           FileNotFoundException
    {        
        File                    ftemp;
        String                  fileName= m_packName + ".deb";
        File                    filePack= new File( p, fileName );
        long                    sysTime = System.currentTimeMillis( );
        FileOutputStream        fos = new FileOutputStream( filePack );
        BufferedOutputStream    bos = new BufferedOutputStream( fos );
        ArArchiveOutputStream   aros= new ArArchiveOutputStream( bos );
        byte[]                  debBin = { (byte)'2', (byte)'.', (byte)'0', (byte)0x0a };

        // Write the file 'debian-binary'
        aros.putArchiveEntry( new ArArchiveEntry( "debian-binary",
                                                  4,
                                                  0,
                                                  0,
                                                  0x81a4,
                                                  sysTime ) );
        aros.write( debBin );
        aros.closeArchiveEntry( );

        // Create control.tar.gz
        ftemp = File.createTempFile( sysTime+"", "control.tar.gz" );
        doCreateControlTarGZip( ftemp );
        aros.putArchiveEntry( new ArArchiveEntry( "control.tar.gz",
                                                  ftemp.length( ),
                                                  0,
                                                  0,
                                                  0x81a4,
                                                  sysTime ) );
        BuilderUtil.getInstance( ).copyFileToOutputStream( aros, ftemp );
        aros.closeArchiveEntry( );
        ftemp.delete( );

        // Create data.tar.gz
        ftemp = File.createTempFile( sysTime+"", "data.tar.gz" );
        doAddFilesToTarGZip( ftemp );
        aros.putArchiveEntry( new ArArchiveEntry( "data.tar.gz",
                                                  ftemp.length( ),
                                                  0,
                                                  0,
                                                  0x81a4,
                                                  sysTime ) );
        BuilderUtil.getInstance( ).copyFileToOutputStream( aros, ftemp );
        aros.closeArchiveEntry( );
        ftemp.delete( );

        // Done
        aros.close( );
        bos.close( );
        fos.close( );

        // Return absolute path
        return filePack.getName( );
    }


    /**
     *
     * @param os
     * @throws IOException
     */
    private void doCreateControlTarGZip ( File f )
    throws Exception
    {
        File                        ftemp;
        FileOutputStream            os   = new FileOutputStream( f );
        GzipCompressorOutputStream  gzos = new GzipCompressorOutputStream( os );
        TarArchiveOutputStream      tos  = new TarArchiveOutputStream( gzos );

        // Write control file
        ftemp = File.createTempFile( System.currentTimeMillis()+"", "control" );
        doWriteControl( ftemp );
        tos.putArchiveEntry( new TarArchiveEntry( ftemp, "./control" ) );
        BuilderUtil.getInstance( ).copyFileToOutputStream( tos, ftemp );
        tos.closeArchiveEntry( );
        ftemp.delete( );

        // Write md5sums
        ftemp = File.createTempFile( System.currentTimeMillis()+"", "md5sums" );
        doWriteMD5SumsToFile( ftemp );
        tos.putArchiveEntry( new TarArchiveEntry( ftemp, "./md5sums" ) );
        BuilderUtil.getInstance( ).copyFileToOutputStream( tos, ftemp );
        tos.closeArchiveEntry( );
        ftemp.delete( );

        // Add prerm, postrm, preinst, postinst scripts
        for ( Entry<String, String> s : m_scriptMap.entrySet( ) )
        {
            TarArchiveEntry e = new TarArchiveEntry( "./"+s.getKey( ) );
            e.setSize( s.getValue( ).length( ) );
            tos.putArchiveEntry( e );
            BuilderUtil.getInstance( ).copyStringToOutputStream( tos, s.getValue( ) );
            tos.closeArchiveEntry( );
        }

        // Done
        tos.close( );
        gzos.close( );
        os.close( );
    }

    /**
     *
     * @param os
     */
    private void doWriteControl ( File f )
    throws Exception
    {
        Vector<Header>        order = new Vector<Header>( 20 );
        FileOutputStream      fos   = new FileOutputStream( f );
        BufferedOutputStream  bos   = new BufferedOutputStream( fos );

        // Add Installed-Size header
        addHeader( new SizeHeader( m_installedSize ) );

        // FIXME: Replace with priority queue
        for ( int i = 0; i < 20; i++ )
            order.add( null );

        // Check if the mandatory headers are there
        for ( String s : Header.getMandatory( ) )
            if ( m_headerMap.containsKey( s ) == false )
                throw new Exception( "Mandatory header '" + s + "' is missing" );

        // Put the headers in a priority queue to get correct order
        for ( Header h : m_headerMap.values( ) )
            order.set( h.getPriority( ), h );

        // Write them
        for ( Header h : order )
        {
            if ( h == null )
                continue;

            bos.write( h.toString( ).getBytes( ) );
            bos.write( 0x0a );
        }

        bos.close( );
        fos.close( );
    }



    /**
     * Adds the files in the file list in a tar+gz
     *
     * @param o Output file
     *
     * @throws IOException If error occurs during writing
     * @throws FileNotFoundException If the output file could not be opened.
     */
    private void doAddFilesToTarGZip ( File o )
    throws IOException,
           FileNotFoundException
    
    {
        FileOutputStream            os   = new FileOutputStream( o );
        GzipCompressorOutputStream  gzos = new GzipCompressorOutputStream( os );
        TarArchiveOutputStream      tos  = new TarArchiveOutputStream( gzos );

        // Add files
        for ( SimpleEntry<File, SimpleEntry<String, Integer>> fileEntry : m_fileList )
        {
            File   file = fileEntry.getKey( );
            String name = fileEntry.getValue( ).getKey( );
            int    mode = fileEntry.getValue( ).getValue( );
            TarArchiveEntry e = new TarArchiveEntry( file, name );

            // Add to tar, user/group id 0 is always root
            e.setMode( mode );
            e.setUserId( 0 );
            e.setUserName( "root" );
            e.setGroupId( 0 );
            e.setGroupName( "root" );     
            tos.putArchiveEntry( e );

            // Write bytes
            if ( file.isFile( ) )
                BuilderUtil.getInstance( ).copyFileToOutputStream( tos, file );
            tos.closeArchiveEntry( );
        }

        // Done
        tos.close( );
        gzos.close( );
        os.close( );
    }

    
    /**
     * Writes the md5 sums of the files in the package to a file.
     *
     * @param o Output file
     * @throws IOException If there occurs an error during writing
     * @throws FileNotFoundException If failed to open output file
     */
    private void doWriteMD5SumsToFile ( File o )
    throws IOException,
           FileNotFoundException
    {
        FileOutputStream fos = new FileOutputStream( o );
        fos.write( m_md5sums.toString( ).getBytes( ) );
        fos.close( );
    }
}
