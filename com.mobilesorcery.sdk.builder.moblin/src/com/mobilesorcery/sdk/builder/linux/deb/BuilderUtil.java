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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

/**
 * A singleton class which provides helper methods for building
 * linux packages.
 *
 * @author Ali Mosavian
 */
public class BuilderUtil
{
    private byte[]  m_copyBuffer;

    /**
     * Private constructor
     *
     */
    private BuilderUtil ( )
    {
        m_copyBuffer= new byte[4*1024];
    }

    /**
     * Calculates the combined size of a directory tree/structure
     *
     * @param i The tree to calculate for
     *
     * @throws IOException If there's an I/O error
     */
    public long calcCombinedSize ( File i )
    {
        // Is it a file?
        if ( i.isFile( ) == true )
            return i.length( );

        // It's a directory
        long size = 0;
        for ( File f : i.listFiles( ) )
            size += calcCombinedSize( f );
        return size;
    }


    /**
     * Adds a directory structure to a gzipped tar.
     *
     * @param o The output file that the tar+gz will be written to
     * @param i The directory to add
     *
     * @throws IOException If there's an I/O error
     */
    public void tarGZipDirectory ( File o, File i )
    throws IOException
    {
        FileOutputStream            fos  = new FileOutputStream( o );
        GzipCompressorOutputStream  gzos = new GzipCompressorOutputStream( fos );
        TarArchiveOutputStream      tos  = new TarArchiveOutputStream( gzos );

        // Recursivly add to tar
        doAddFileToTar( tos, "./", i );

        // Close
        tos.close( );
        gzos.close( );
        fos.close( );
    }


    /**
     * Recursivly add a directory structure to a tar output stream
     *
     * @param t Tar output stream
     * @param r Relative path up to this point
     * @param c Current file
     */
    private void doAddFileToTar ( TarArchiveOutputStream t,
                                  String r,
                                  File c )
    throws IOException
    {
        // Update relative path
        r += (r.isEmpty( ) ? "" : (r.endsWith( "/") ? "" : "/")) + c.getName( );

        // Is it a file?
        if ( c.isFile( ) == true )
        {
            ArchiveEntry e = t.createArchiveEntry( c, r );
            t.putArchiveEntry( e );
            copyFileToOutputStream( t, c );
            t.closeArchiveEntry( );
            return;
        }

        // It's a directory
        for ( File f : c.listFiles( ) )
            doAddFileToTar( t, r, f );
    }

    /**
     * Simple method for copying file bytes to output stream
     *
     * @param t
     * @param f
     */
    public void copyFileToOutputStream ( OutputStream t,
                                         File f )
    throws FileNotFoundException, IOException
    {
        int             read;
        int             size = ( int )f.length( );
        FileInputStream fis  = new FileInputStream( f );

        while ( size > 0 )
        {
            read = fis.read( m_copyBuffer, 0, Math.min( size, m_copyBuffer.length ) );
            t.write( m_copyBuffer, 0, read );
            size -= read;
        }
    }


    /**
     * Simple method for copying String bytes to output stream
     *
     * @param t
     * @param s
     */
    public void copyStringToOutputStream ( OutputStream t,
                                           String s )
    throws IOException
    {
        t.write( s.getBytes( ) );
    }


    /**
     * Simple method for copying file bytes to output stream
     *
     * @param f File to output
     * @param i Inputstream
     * @param b Bytes to copy
     */
    public void copyInputStreamToFile ( File f,
                                        InputStream i,
                                        long b )
    throws FileNotFoundException, IOException
    {
        int written;
        int size = (int)b;
        FileOutputStream fos = new FileOutputStream( f );

        while ( size > 0 )
        {
            written = i.read( m_copyBuffer, 0, Math.min( size, m_copyBuffer.length ) );
            fos.write( m_copyBuffer, 0, written );
            size -= written;
        }
        fos.close( );
    }

    /**
     * Simple method for copying file bytes to output stream
     *
     * @param t
     * @param f
     */
    public void copyFile ( File o,
                           File i )
    throws FileNotFoundException, IOException
    {
        int             read;
        int             size = ( int )i.length( );
        FileInputStream  fis  = new FileInputStream( i );
        FileOutputStream fos = new FileOutputStream( o );

        while ( size > 0 )
        {
            read = fis.read( m_copyBuffer, 0, Math.min( size, m_copyBuffer.length ) );
            fos.write( m_copyBuffer, 0, read );
            size -= read;
        }
    }


    /**
     * Adds a directory structure to a gzipped tar.
     *
     * @param o The output file that the tar+gz will be written to
     * @param i The directory to add
     *
     * @throws IOException If there's an I/O error
     */
    public void calcMD5Directory ( File o,
                                     File i )
    throws IOException,
           NoSuchAlgorithmException
    {
        FileOutputStream    fos;
        StringBuilder       sb = new StringBuilder( );

        // Recursivly calculate md5 sums for directory structure
        doAddFileToMD5Sums( sb, "", i );

        // Write
        fos = new FileOutputStream( o );
        fos.write( sb.toString( ).getBytes( ) );
        fos.close( );
    }


    /**
     * Adds a directory structure to a gzipped tar.
     *
     * @param o The output file that the tar+gz will be written to
     * @param i The directory to add
     *
     * @throws IOException If there's an I/O error
     */
    private void doAddFileToMD5Sums ( StringBuilder o,
                                      String r,
                                      File c )
    throws IOException,
           FileNotFoundException,
           NoSuchAlgorithmException

    {
        // Update relative path
        r += (r.isEmpty( ) ? "" : (r.endsWith( "/") ? "" : "/")) + c.getName( );

        // Is it a file?
        if ( c.isFile( ) == true )
        {
            String md5 = calcFileMD5Sum( c );
            o.append( md5 );
            for ( int i = 0; i < 1+(32-md5.length( )); i++ )
                o.append( " " );
            o.append( r )
             .append( "\n" );
            return;
        }

        // It's a directory
        for ( File f : c.listFiles( ) )
            doAddFileToMD5Sums( o, r, f );
    }


    /**
     * Calculates the md5 has for a file and returns the
     * hash as a hex string
     *
     * @param f File to hash
     * @return 128 bit hash in hex
     *
     * @throws NoSuchAlgorithmException
     * @throws FileNotFoundException
     * @throws IOException
     */
    public String calcFileMD5Sum ( File f )
    throws NoSuchAlgorithmException,
           FileNotFoundException,
           IOException
    {
        MessageDigest   m   = MessageDigest.getInstance( "MD5" );
        FileInputStream fos = new FileInputStream( f );

        // Hash file
        int size = (int)f.length( );
        while ( size > 0 )
        {
            int count = fos.read( m_copyBuffer );
            m.update( m_copyBuffer, 0, count );
            size -= count;
        }
        fos.close( );

        BigInteger hash = new BigInteger( 1, m.digest( ) );
        return hash.toString( 16 );
    }

    /**
     * Returns singleton instance
     * Note: Not thread safe
     *
     */
    private static BuilderUtil m_instance = null;
    public  static BuilderUtil getInstance ( )
    {
        if ( m_instance == null )
            m_instance = new BuilderUtil( );
        return m_instance;
    }

}
