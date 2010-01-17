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
 * Represents the package version in the debian package
 * Note: This header is mandatory final package
 * 
 * @author Ali Mosavian
 */
public class VersionHeader
extends Header
{
    private String m_version;

    /**
     * Constructor, sets package version
     *
     * @param v Package version
     */
    public VersionHeader ( String v )
    {
        m_version = v;
    }

    /**
     * Returns the string representation of the header
     *
     * @return Header name
     */
    @Override
    public String getName ( )
    {
        return "Version";
    }

    /**
     * Converts to its string representation
     *
     */
    @Override
    public String toString ( )
    {
        return "Version: " + m_version;
    }
}
