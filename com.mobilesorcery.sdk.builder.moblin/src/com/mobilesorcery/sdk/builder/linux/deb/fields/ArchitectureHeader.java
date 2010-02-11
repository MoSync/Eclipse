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
 * This Header represents the 'Architecture' field in the debian package.
 * Note: This field is mandatory

 * @author Ali Mosavian
 */
public class ArchitectureHeader
extends Header
{
    public enum CpuArch
    {
        Any ("all"),
        Alpha ("alpha"),
        Amd64 ("amd64"),
        Arm ("arm"),
        Armel ("armel"),
        HPPA ("hppa"),
        I386 ("i386"),
        IA64 ("ia64"),
        Mips ("mips"),
        Mipsel ("mipsel"),
        PowerPC ("powerpc"),
        S390 ("s390"),
        Sparc ("sparc");

        private final String m_name;
        CpuArch ( String n ) { m_name = n; };
        public String toString ( ) { return m_name; };
    }

    private String m_arch;


    /**
     * Constructor
     *
     * @param a Binary package architecture
     */
    public ArchitectureHeader ( CpuArch a )
    {
        m_arch = a.toString( );
    }

    /**
     * Returns the string representation of the header
     *
     * @return Header name
     */
    @Override
    public String getName ( )
    {
        return "Architecture";
    }

    /**
     * Returns the string value of the header
     *
     * @return Property value
     */
    public String getValue ( )
    {
        return m_arch;
    }

    /**
     * Converts to its string representation
     *
     */
    @Override
    public String toString ( )
    {
        return "Architecture: " + m_arch;
    }
}
