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

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Map.Entry;

public class SLDInfo {

    public static final int UNKNOWN_LINE = -1;

    private TreeMap<Integer, String> fileTable = new TreeMap<Integer, String>();
    private TreeMap<Integer, Integer> addrToFile = new TreeMap<Integer, Integer>();
    private TreeMap<Integer, Integer> addrToLine = new TreeMap<Integer, Integer>();
    private File file;

    SLDInfo(File sldFile) {
        this.file = sldFile;
    }

    void addFile(int id, String name) {
        fileTable.put(id, name);
    }

    void addLocationForAddress(int addr, int lineInFile, int fileId) {
        addrToLine.put(addr, lineInFile);
        addrToFile.put(addr, fileId);
    }

    public String getFileName(int addr) {
        if (addrToFile.isEmpty()) {
            return null;
        }

        Entry<Integer, Integer> entry = addrToFile.floorEntry(addr);
        if (entry != null) {
            Integer fileId = entry.getValue();
            if (fileId != null) {
                return fileTable.get(fileId);
            }
        }

        return null;
    }

    public int getLine(int addr) {
        if (addrToLine.isEmpty()) {
            return UNKNOWN_LINE;
        }

        Entry<Integer, Integer> entry = addrToLine.floorEntry(addr - 1);
        if (entry != null) {
            Integer line = entry.getValue();
            if (line != null) {
                return line;
            }
        }

        return UNKNOWN_LINE;
    }

    public void write(Writer writer) throws IOException {
        writer.write(SLDParser.FILE_MARKER);
        writer.write('\n');
        for (Iterator<Integer> files = fileTable.keySet().iterator(); files.hasNext();) {
            Integer fileId = files.next();
            String filename = fileTable.get(fileId);
            writer.write(fileId + ":" + filename);
            writer.write('\n');
        }

        writer.write(SLDParser.SLD_MARKER);
        writer.write('\n');

        for (Iterator<Integer> addrs = addrToFile.keySet().iterator(); addrs.hasNext();) {
            Integer addr = addrs.next();
            int line = addrToLine.get(addr);
            int file = addrToFile.get(addr);

            writer.write(Integer.toHexString(addr) + ":" + line + ":" + file);
            writer.write("\n");
        }
    }

    public File getSLDFile() {
        return file;
    }
 
}
