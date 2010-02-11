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
package com.mobilesorcery.sdk.builder.linux.deb.fields;

/**
 *
 * @author Ali Mosavian
 */
public class DependsHeader
extends Header
{
    public enum VersionRelation
    {
        Any ("any"),
        Equal ("="),
        Less ("<"),
        LessOrEqual ("<="),
        Greater (">"),
        GreaterOrEqual (">=");

        private final String m_rel;
        VersionRelation ( String n ) { m_rel = n; };
        public String toString ( ) { return m_rel; };
    };

    private String  m_library;
    
    /**
     * Constructor
     *
     */
    public DependsHeader ( String l )
    {
        m_library = l;
    }

    /**
     * Constructor
     *
     */
    public DependsHeader ( String l,
                           String v,
                           VersionRelation r )
    {
        m_library = l;
        if ( r.equals( r.Any ) == false )        
            m_library += " (" +r.toString( )+ " " + v + ")";
    }

    /**
     * Returns the string representation of the header
     *
     * @return Header name
     */
    @Override
    public String getName ( )
    {
        return "Depends";
    }

    /**
     * Converts to its string representation
     *
     */
    @Override
    public String toString ( )
    {
        String s = "Depends: " + m_library;

        DependsHeader h = (DependsHeader)m_next;
        while ( h != null )
        {
            s += ", " + h.m_library;
            h = (DependsHeader)h.m_next;
        }

        return s;
    }

    /**
     * Returns weather this header type can have multiple
     * values.
     *
     * @return true or false
     */
    @Override
    public boolean isMultiValue ( )
    {
        return true;
    }

    /**
     * Links another header to the end of this one.
     *
     * @throws Exception if this is not a multi-value type header
     */
    @Override
    public void addNext ( Header h )
    throws Exception
    {
        add( h );
    }
}
