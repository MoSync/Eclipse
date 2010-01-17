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

import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;


/**
 * This is a simple class that was refectored out or PackageParser,
 * it handles variables and resolving them in strings and files.
 *
 * @author Ali Mosavian
 */
public class VariableResolver
{
    private Map<String, String>     m_variableMap;

    /**
     * Constructor
     */
    public VariableResolver ( )
    {
        m_variableMap = new HashMap<String, String>( );
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
        m_variableMap.put( s.toLowerCase( ), v );
    }

    /**
     * Parse an input stream and resolves variables. Result
     * is parsed string.
     *
     * @param i Input stream to parse
     *
     * @throws Exception If recursion is too deep or a variable isn't defined
     * @throws IOException Error while reading from stream
     */
    public String doParseStream ( InputStream i )
    throws Exception,
           IOException
    {
        StringBuilder res = new StringBuilder( );
        BufferedReader r = new BufferedReader( new InputStreamReader( i ) );

        // Read and parse
        while ( r.ready( ) == true )
            res.append( doResolveString( r.readLine( ) )+"\n" );

        return res.toString( );
    }

    /**
     * Copies an inputstream to a file, recursivly resolving
     * variables while copying.

     * @param o Output file
     * @param i Input stream to parse and copy
     *
     * @throws Exception If recursion is too deep or a variable isn't defined
     * @throws IOException Error while reading from stream/writing to file
     */
    public void doParseCopyStream ( File o,
                                    InputStream i )
    throws Exception,
           IOException
    {
        BufferedReader r = new BufferedReader( new InputStreamReader( i ) );
        BufferedWriter w = new BufferedWriter( new BufferedWriter( new FileWriter( o ) ));

        // Copy and parse
        while ( r.ready( ) == true )
            w.write( doResolveString( r.readLine( ) )+"\n" );

        // Close output
        w.close( );
    }
    

    /**
     * Processes a string recursivly replacing all variables with their
     * values.
     *
     * @param s String to process
     *
     * @return Resolved string
     * @throws Exception If recursion is too deep or a variable isn't defined
     */
    public String doResolveString ( String s )
    throws Exception
    {
        int state = 0;
        StringBuilder str = new StringBuilder( );
        StringBuilder var = new StringBuilder( );

        // State machine for extracting variables
        for ( char c : s.toCharArray( ) )
        {
            switch ( state )
            {
                case 0:
                    if ( c == '%' )
                        state = 1;
                    else
                        str.append( c );

                    break;
                case 1:
                    if ( c == '%' )
                    {
                        str.append( doResolveVariable( var.toString( ) ) );
                        var = new StringBuilder( );
                        state = 0;
                    }
                    else
                        var.append( c );

                    break;
            }
        }

        return str.toString( );
    }

    /**
     * Recursivly resolves a variable to its value.
     *
     * @param s Variable name
     *
     * @return Variable value
     * @throws Exception If recursion is too deep or a variable isn't defined
     */
    private String doResolveVariable ( String s )
    throws Exception
    {
        return doResolveVariable( s, 0 );
    }

    /**
     * Recursivly resolves a variable to its value.
     *
     * @param s Variable name
     * @param d Current recursion depth
     *
     * @return Variable value
     * @throws Exception If recursion is too deep or a variable isn't defined
     */
    private String doResolveVariable ( String s,
                                       int d )
    throws Exception
    {
        if ( d > 32 )
            throw new Exception( "Variable resolve - recursion too deep" );

        s = s.toLowerCase( );
        if ( m_variableMap.containsKey( s ) == false )
            throw new Exception( "Variable resolve - undefined variable" );

        String val = m_variableMap.get( s );
        if ( val.charAt( 0 ) == '%' )
            return doResolveVariable( val.replaceAll( "%", "" ), d+1 );

        return val;
    }
}
