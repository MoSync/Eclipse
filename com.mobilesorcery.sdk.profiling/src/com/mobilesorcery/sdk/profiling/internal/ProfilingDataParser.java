/*  Copyright (C) 2010 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.profiling.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.mobilesorcery.sdk.core.ParseException;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.profiling.IInvocation;
import com.mobilesorcery.sdk.profiling.Invocation;

public class ProfilingDataParser extends DefaultHandler {

    public IInvocation parse(File input) throws IOException, ParseException {
        FileInputStream inputStream = new FileInputStream(input);
        try {
            return parse(inputStream);
        } finally {
            Util.safeClose(inputStream);
        }
    }

    public IInvocation parse(InputStream input) throws IOException, ParseException {
        Invocation root = new Invocation(null);
        ProfilingDataParserHandler handler = new ProfilingDataParserHandler(root);
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setValidating(false);
            spf.setNamespaceAware(true);
            SAXParser sp = spf.newSAXParser();
            final XMLReader xr = sp.getXMLReader();
            xr.setContentHandler(handler);
            xr.parse(new InputSource(input));
            return root;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ParseException(e);
        }
    }
}
