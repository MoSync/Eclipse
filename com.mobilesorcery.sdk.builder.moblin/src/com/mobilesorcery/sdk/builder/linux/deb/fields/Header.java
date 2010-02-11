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
 * Base header class
 * Note: See http://www.debian.org/doc/debian-policy/ch-controlfields.html
 *
 * @author Ali Mosavian
 */
public abstract class Header
{
    private int       m_prio;
    protected Header  m_next;

    /**
     * Constructor
     *
     */
    public Header ( )
    {
        m_next = null;
        m_prio = -1;
    }

    /**
     * Returns the string representation of the header
     *
     * @return Header name
     */
    abstract public String getName ( );

    /**
     * Returns the priority (order) of the field
     *
     * @return Priority
     */
    public int getPriority ( )
    {
        if ( m_prio == -1 )
            m_prio = Header.getHeaderPriority( getName( ) );
        
        return m_prio;
    }

    /**
     * Returns weather this header type can have multiple
     * values.
     *
     * @return true or false
     */
    public boolean isMultiValue ( )
    {
        return false;
    }

    /**
     * Links another header to the end of this one.
     *
     * @throws Exception if this is not a multi-value type header
     */
    public void addNext ( Header h )
    throws Exception
    {
        throw new Exception( "Not a multi-value header" );
    }

    /**
     * In case of a multi value header, this will return the next
     * header, or null if no more headers.
     *
     * @return Header or null
     */
    public Header getNext ( )
    {
        return null;
    }

    /**
     * Private method for linking together headers,
     * this is only to be used by sub class if needed.
     *
     * @param n Header to add to end of list.
     */
    protected void add ( Header n )
    {
        Header e = this;

        while ( e.m_next != null )
            e = e.m_next;

        e.m_next = n;
    }


    /**
     * Returns header priority (order)
     *
     * @param f Header name
     * @return Priority or 0xffff if not found
     */
    public static String[] getMandatory ( )
    {
        String[] r = { "Package",
                       "Version",
                       "Architecture",
                       "Maintainer",
                       "Description" };
       return r;
    }

    /**
     * Returns header priority (order)
     *
     * @param f Header name
     * @return Priority or 0xffff if not found
     */
    private static int getHeaderPriority ( String f )
    {
       for ( int i = 0; i < m_priorities.length; i++ )
           if ( m_priorities[i].equalsIgnoreCase( f ) == true )
               return i+1;

       return 0xffff;
    }

    private static String[] m_priorities =
    {
        "Package",
        "Version",
        "Architecture",
        "Maintainer",
        "Installed-Size",
        "Depends",
        "Replaces",
        "Section",
        "Homepage",
        "Priority",
        "Description"
     };
}
