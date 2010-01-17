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
public class SectionHeader
extends Header
{
    /**
     * Sections in Debian
     */
    public enum DebianSections
    {
        Admin ("admin"),
        Mono ("cli-mono"),
        Comm ("comm"),
        Database ("database"),
        Devel ("devel"),
        Debug ("debug"),
        Doc ("doc"),
        Editors ("editors"),
        Electronics ("electronics"),
        Embedded ("embedded"),
        Fonts ("fonts"),
        Games ("games"),
        Gnome ("gnome"),
        Graphics ("graphics"),
        Gnu_R ("gnu-r"),
        Gnustep ("gnustep"),
        Hamradio ("hamradio"),
        Haskell ("haskell"),
        Httpd ("httpd"),
        Interpreters ("interpreters"),
        Java ("java"),
        Kde ("kde"),
        Kernel ("kernel"),
        Libs ("libs"),
        Libdevel ("libdevel"),
        Lisp ("lisp"),
        Localization ("localization"),
        Mail ("mail"),
        Math ("math"),
        Misc ("misc"),
        Net ("net"),
        News ("news"),
        OCaml ("ocaml"),
        Oldlibs ("oldlibs"),
        Otherosfs ("otherosfs"),
        Perl ("perl"),
        Php ("php"),
        Python ("python"),
        Ruby ("ruby"),
        Science ("science"),
        Shells ("shells"),
        Sound ("sound"),
        Tex ("tex"),
        Text ("text"),
        Utils ("utils"),
        VCS ("vcs"),
        Video ("video"),
        Web ("web"),
        X11 ("x11"),
        Xfce ("xfce"),
        Zope ("zope");

        private final String m_name;
        DebianSections ( String n ) { m_name = n; };
        public String toString ( ) { return m_name; };
    };

    private String m_value;

    /**
     * Constructor
     *
     */
    public SectionHeader ( DebianSections s )
    {
        m_value = s.toString( );
    }


    /**
     * Returns the string representation of the header
     *
     * @return Header name
     */
    @Override
    public String getName ( )
    {
        return "Section";
    }


    /**
     * Converts to its string representation
     *
     */
    public String toString ( )
    {
        return "Section: " + m_value;
    }
}
