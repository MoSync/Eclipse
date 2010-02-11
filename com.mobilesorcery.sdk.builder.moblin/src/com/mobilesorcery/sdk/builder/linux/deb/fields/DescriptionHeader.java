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
public class DescriptionHeader
extends Header
{
    private String m_summary;
    private String m_description;


    /**
     * Constructor
     *
     * @param s Summary, 80 columns max
     * @param d Long description
     */
    public DescriptionHeader ( String s,
                               String d )
    throws Exception
    {
        if ( s.length( ) > 80 )
            throw new Exception( "Summary too long" );

        m_summary     = s;
        m_description = d;
    }

    /**
     * Returns the string representation of the header
     *
     * @return Header name
     */
    @Override
    public String getName ( )
    {
        return "Description";
    }


    /**
     * Converts to its string representation
     *
     */
    @Override
    public String toString ( )
    {
        String d = "Description: " + m_summary + "\n";

        for ( String s : m_description.split( "\n" ) )
            d += " " + s + "\n";

        return d;
    }
}
