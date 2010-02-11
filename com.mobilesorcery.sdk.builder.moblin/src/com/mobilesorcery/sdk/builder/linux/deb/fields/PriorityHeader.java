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
 * Represents the priority field in the debian package.
 * See http://www.debian.org/doc/debian-policy/ch-archive.html#s-priorities
 *
 * Note: This header is optional in the final package
 *
 * @author Ali Mosavian
 */
public class PriorityHeader
extends Header
{
    public enum Priorities
    {
        Required ("required"),
        Important ("important"),
        Standard ("standard"),
        Optional ("optional"),
        Extra ("extra");

        private final String m_name;
        Priorities ( String n ) { m_name = n; };
        public String toString ( ) { return m_name; };
    }

    private String m_priority;


    /**
     * Constructor
     *
     * @param a Binary package architecture
     */
    public PriorityHeader ( Priorities a )
    {
        m_priority = a.toString( );
    }

    /**
     * Returns the string representation of the header
     *
     * @return Header name
     */
    @Override
    public String getName ( )
    {
        return "Priority";
    }

    /**
     * Converts to its string representation
     *
     */
    @Override
    public String toString ( )
    {
        return "Priority: " + m_priority;
    }
}
