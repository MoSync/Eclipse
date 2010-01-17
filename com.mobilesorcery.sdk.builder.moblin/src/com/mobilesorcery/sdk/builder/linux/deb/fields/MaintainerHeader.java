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
public class MaintainerHeader
extends Header
{
    private String m_name;
    private String m_email;


    /**
     * Constructor
     *
     * @param n Name of maintainer
     * @param e Maintainer email
     */
    public MaintainerHeader ( String n,
                              String e )
    throws Exception
    {
        m_name  = n.replaceAll( "\n", " " );
        m_email = e.replaceAll( "\n", " " );
    }

    /**
     * Returns the string representation of the header
     *
     * @return Header name
     */
    @Override
    public String getName ( )
    {
        return "Maintainer";
    }

    /**
     * Converts to its string representation
     *
     */
    @Override
    public String toString ( )
    {
        return "Maintainer: " +
                m_name + " <" +
                m_email + ">";
    }
}
