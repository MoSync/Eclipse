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
package com.mobilesorcery.sdk.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.text.MessageFormat;
import java.util.ArrayList;

public class SLDParser {

    static final String FILE_MARKER = "Files";
    static final String SLD_MARKER = "SLD";
    private static final int FILE_STATE = 1;
    private static final int SLD_STATE = 2;

    private int state;

    private ArrayList<Exception> errors = new ArrayList<Exception>();
    private SLDInfo sld;

    private int currentLine;

    public void parse(File sldFile) throws IOException {
        BufferedReader sldReader = new BufferedReader(new FileReader(sldFile));
        try {
            sld = new SLDInfo(sldFile);
            currentLine = 0;
            LineNumberReader sldLines = new LineNumberReader(sldReader);
            for (String line = sldLines.readLine(); line != null; line = sldLines.readLine()) {
                line = line.trim();
                // Lines are 1-based.
                currentLine++;
                if (FILE_MARKER.equals(line)) {
                    state = FILE_STATE;
                } else if (SLD_MARKER.equals(line)) {
                    state = SLD_STATE;
                } else if (line.length() > 0){
                    parseEntry(line);
                }
            }
        } finally {
            if (sldReader != null) {
                sldReader.close();
            }
        }
    }

    public SLDInfo getSLD() {
        return sld;
    }

    public String[] getErrors() {
        return errors.toArray(new String[0]);
    }

    private void parseEntry(String line) {
        try {
            if (state == FILE_STATE) {
                parseFileEntry(line);
            } else if (state == SLD_STATE) {
                parseSLDEntry(line);
            }
        } catch (SLDParseException e) {
            errors.add(e);
        }
    }

    private void parseSLDEntry(String line) throws SLDParseException {
        String[] sldEntry = line.split(":", 3);
        if (sldEntry.length != 3) {
            throw new SLDParseException(MessageFormat.format("Invalid line: {0}", line), currentLine);
        }
        
        int addr = parseInt(sldEntry[0], 16);
        int lineInFile = parseInt(sldEntry[1], 10);
        int fileId = parseInt(sldEntry[2], 10);
        
        sld.addLocationForAddress(addr, lineInFile, fileId);
    }

    private void parseFileEntry(String line) throws SLDParseException {
        String[] fileEntry = line.split(":", 3);
        if (fileEntry.length != 3) {
            throw new SLDParseException(MessageFormat.format("Invalid line: {0}", line), currentLine);
        }

        String id = fileEntry[0];
        String name = fileEntry[2];

        int idValue = parseInt(id, 10);
        
        sld.addFile(idValue, name);
    }

    private int parseInt(String str, int radix) throws SLDParseException {
    	return parseInt(str, radix, currentLine);
    }
    
    public static int parseInt(String str, int radix, int currentLine) throws SLDParseException {
        try {
            return Integer.parseInt(str, radix);
        } catch (Exception e) {
            throw new SLDParseException(MessageFormat.format("Expected a number, found {0}", str), currentLine);
        }
    }
}
