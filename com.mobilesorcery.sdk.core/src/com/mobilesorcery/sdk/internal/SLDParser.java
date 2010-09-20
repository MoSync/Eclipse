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

import org.eclipse.cdt.utils.CPPFilt;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncTool;


public class SLDParser {

    public static final String FILE_MARKER = "Files";
    public static final String SLD_MARKER = "SLD";
    public static final String FUNCTIONS_MARKER = "FUNCTIONS";
    public static final String LINE_IP_MARKER = "LINEIP";
     
    private static final int FILE_STATE = 1;
    private static final int SLD_STATE = 2;
    private static final int FUNCTIONS_STATE = 3;
    private static final int LINE_IP_STATE = 4;
    
    private int state;

    private ArrayList<Exception> errors = new ArrayList<Exception>();
    private SLDInfoImpl sld;

    private int currentLine;
    private CPPFilt cppFilt;

    public void parse(File sldFile) throws IOException {
        BufferedReader sldReader = new BufferedReader(new FileReader(sldFile));
        try {
            try {
                cppFilt = new CPPFilt(MoSyncTool.getDefault().getBinary("c++filt").toOSString());
            } catch (IOException e) {
                // Ignore but log.
                CoreMoSyncPlugin.getDefault().logOnce(e, getClass().getName() + "c++filt");
            }
            sld = new SLDInfoImpl(sldFile);
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
                } else if (FUNCTIONS_MARKER.equals(line)) { 
                    state = FUNCTIONS_STATE;
                } else if (LINE_IP_MARKER.equals(line)) { 
                    state = LINE_IP_STATE;
                } else if (line.length() > 0){
                    parseEntry(line);
                }
            }
        } finally {
            if (sldReader != null) {
                sldReader.close();
            }
            if (cppFilt != null) {
                cppFilt.dispose();
            }
        }
    }

    public SLDInfoImpl getSLD() {
        return sld;
    }

    public String[] getErrors() {
        return errors.toArray(new String[0]);
    }

    private void parseEntry(String line) {
        try {
            switch (state) {
            case FILE_STATE:
                parseFileEntry(line);
                break;
            case SLD_STATE:
                parseSLDEntry(line);
                break;
            case FUNCTIONS_STATE:
                parseFunctionEntry(line);
                break;
            }
        } catch (SLDParseException e) {
            errors.add(e);
        }
    }

    private void parseFunctionEntry(String line) {
        String[] functionEntry = line.split("\\s", 2);
        if (functionEntry.length == 2) {
            String symbol = functionEntry[0];
            String unmangledFunctionName = symbol;
            try {
               unmangledFunctionName = cppFilt.getFunction(symbol);
            } catch (IOException e) {
                // Just present the mangled name
            }
            String addrRange = functionEntry[1];
            sld.addRangeForFunction(unmangledFunctionName, AddressRange.parse(addrRange));
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
